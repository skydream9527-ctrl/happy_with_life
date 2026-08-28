package media

import (
	"fmt"
	"strings"
	"time"

	"github.com/skydream9527-ctrl/xiaoquexing-server/internal/id"
)

type Blob interface {
	Provider() string
	Put(mime, key string, body []byte) error
	Head(key string) (int64, string, error)
	Get(key string) ([]byte, string, error)
	Delete(key string)
}

type Presigner interface {
	Presign(verb, key, contentType string, exp time.Time) (string, error)
}

type Service struct {
	Store      Store
	Blob       Blob
	TicketKey  string
	QuotaBytes int64
	MaxBytes   int64
	Now        func() time.Time
}

func NewService(store Store, blob Blob, ticketKey string, quota, maxPhoto int64) *Service {
	if quota <= 0 {
		quota = DefaultQuotaBytes
	}
	if maxPhoto <= 0 {
		maxPhoto = DefaultMaxPhotoBytes
	}
	return &Service{Store: store, Blob: blob, TicketKey: ticketKey, QuotaBytes: quota, MaxBytes: maxPhoto, Now: time.Now}
}

func (s *Service) IssueSTS(userID string, req STSRequest, publicPath string) (STSResult, error) {
	kind := strings.ToUpper(strings.TrimSpace(req.Type))
	mime := strings.ToLower(strings.TrimSpace(req.MimeType))
	if kind == "" {
		kind = TypePhoto
	}
	ext, ok := ExtFor(mime)
	if !ok {
		return STSResult{}, ErrUnsupported
	}
	if kind == TypePhoto && photoMIME[mime] == "" {
		return STSResult{}, ErrUnsupported
	}
	if kind == TypeVoice && voiceMIME[mime] == "" {
		return STSResult{}, ErrUnsupported
	}
	if req.SizeBytes <= 0 || req.SizeBytes > s.MaxBytes {
		return STSResult{}, ErrTooLarge
	}
	used, err := s.Store.Usage(userID)
	if err != nil {
		return STSResult{}, err
	}
	if used+req.SizeBytes > s.QuotaBytes {
		return STSResult{}, ErrQuota
	}
	now := s.Now().UTC()
	mediaID := id.New()
	key := fmt.Sprintf("u/%s/m/%s%s", userID, mediaID, ext)
	obj := Object{
		ID: mediaID, UserID: userID, Type: kind, ObjectKey: key,
		UploadStatus: StatusPending, MimeType: mime, ReservedBytes: req.SizeBytes,
		SHA256: req.SHA256, Width: req.Width, Height: req.Height,
		CreatedAt: now, UpdatedAt: now,
	}
	if err := s.Store.Insert(obj); err != nil {
		return STSResult{}, err
	}
	exp := now.Add(STSExpire)
	res := STSResult{
		MediaID: mediaID, ObjectKey: key, Method: "PUT",
		Headers: map[string]string{"Content-Type": mime},
		ExpiresAt: exp, Provider: s.Blob.Provider(),
		QuotaUsed: used + req.SizeBytes, QuotaMax: s.QuotaBytes,
	}
	if p, ok := s.Blob.(Presigner); ok && s.Blob.Provider() == "aliyun" {
		u, err := p.Presign("PUT", key, mime, exp)
		if err != nil {
			return STSResult{}, err
		}
		res.UploadURL = u
		return res, nil
	}
	res.UploadURL = strings.TrimRight(publicPath, "/") + "/api/v1/media/upload/" + signTicket(s.TicketKey, mediaID, "p", exp)
	return res, nil
}

func (s *Service) AcceptUpload(ticket string, mime string, body []byte) error {
	mediaID, op, _, ok := parseTicket(s.TicketKey, ticket)
	if !ok || op != "p" {
		return ErrForbidden
	}
	obj, err := s.Store.Get(mediaID)
	if err != nil {
		return err
	}
	if obj == nil || obj.DeletedAt != nil || obj.UploadStatus == StatusDeleted {
		return ErrNotFound
	}
	if obj.UploadStatus != StatusPending {
		return ErrInvalid
	}
	if int64(len(body)) > obj.ReservedBytes || int64(len(body)) > s.MaxBytes {
		return ErrTooLarge
	}
	if mime == "" {
		mime = obj.MimeType
	}
	return s.Blob.Put(mime, obj.ObjectKey, body)
}

func (s *Service) Complete(userID, mediaID string) (*Object, error) {
	obj, err := s.owned(userID, mediaID)
	if err != nil {
		return nil, err
	}
	if obj.UploadStatus == StatusReady {
		return obj, nil
	}
	if obj.UploadStatus == StatusDeleted {
		return nil, ErrNotFound
	}
	n, mime, err := s.Blob.Head(obj.ObjectKey)
	if err != nil {
		now := s.Now().UTC()
		obj.UploadStatus = StatusMissing
		obj.UpdatedAt = now
		_ = s.Store.Update(*obj)
		return obj, ErrMissingBlob
	}
	if n > obj.ReservedBytes || n > s.MaxBytes {
		return nil, ErrTooLarge
	}
	now := s.Now().UTC()
	obj.SizeBytes = n
	obj.ReservedBytes = 0
	obj.UploadStatus = StatusReady
	obj.UpdatedAt = now
	if mime != "" {
		obj.MimeType = mime
	}
	if err := s.Store.Update(*obj); err != nil {
		return nil, err
	}
	return obj, nil
}

func (s *Service) SignDownload(userID, mediaID string, allow func(o Object) bool, publicPath string) (string, time.Time, error) {
	obj, err := s.Store.Get(mediaID)
	if err != nil {
		return "", time.Time{}, err
	}
	if obj == nil || obj.DeletedAt != nil || obj.UploadStatus == StatusDeleted {
		return "", time.Time{}, ErrNotFound
	}
	if obj.UserID != userID && (allow == nil || !allow(*obj)) {
		return "", time.Time{}, ErrForbidden
	}
	if obj.UploadStatus != StatusReady {
		return "", time.Time{}, ErrMissingBlob
	}
	exp := s.Now().UTC().Add(DownloadExpire)
	if p, ok := s.Blob.(Presigner); ok && s.Blob.Provider() == "aliyun" {
		u, err := p.Presign("GET", obj.ObjectKey, "", exp)
		return u, exp, err
	}
	u := strings.TrimRight(publicPath, "/") + "/api/v1/media/content/" + signTicket(s.TicketKey, obj.ID, "g", exp)
	return u, exp, nil
}

func (s *Service) ReadContent(ticket string) (*Object, []byte, error) {
	mediaID, op, _, ok := parseTicket(s.TicketKey, ticket)
	if !ok || op != "g" {
		return nil, nil, ErrForbidden
	}
	obj, err := s.Store.Get(mediaID)
	if err != nil {
		return nil, nil, err
	}
	if obj == nil || obj.UploadStatus != StatusReady {
		return nil, nil, ErrNotFound
	}
	body, mime, err := s.Blob.Get(obj.ObjectKey)
	if err != nil {
		return nil, nil, err
	}
	if mime != "" {
		obj.MimeType = mime
	}
	return obj, body, nil
}

func (s *Service) Delete(userID, mediaID string) error {
	obj, err := s.owned(userID, mediaID)
	if err != nil {
		return err
	}
	if obj.RecordID != "" {
		return ErrInUse
	}
	now := s.Now().UTC()
	obj.UploadStatus = StatusDeleted
	obj.DeletedAt = &now
	obj.ReservedBytes = 0
	obj.UpdatedAt = now
	if err := s.Store.Update(*obj); err != nil {
		return err
	}
	s.Blob.Delete(obj.ObjectKey)
	return nil
}

func (s *Service) ListByRecord(recordID string) ([]Item, error) {
	rows, err := s.Store.ListByRecord(recordID)
	if err != nil {
		return nil, err
	}
	out := make([]Item, 0, len(rows))
	for _, o := range rows {
		out = append(out, o.Item())
	}
	return out, nil
}

func (s *Service) Get(id string) (*Object, error) { return s.Store.Get(id) }

func (s *Service) Quota(userID string) (used, max int64, err error) {
	used, err = s.Store.Usage(userID)
	return used, s.QuotaBytes, err
}

// Resolve implements ledger.MediaHook: PHOTO/VOICE must already be READY and owned.
func (s *Service) Resolve(userID, recordID string, types []struct {
	MediaID string
	Type    string
}) (photoCount int, hasVoice bool, ids []string, err error) {
	ids = make([]string, 0, len(types))
	for _, in := range types {
		kind := strings.ToUpper(strings.TrimSpace(in.Type))
		if kind == TypePhoto || kind == TypeVoice {
			if in.MediaID == "" {
				return 0, false, nil, ErrInvalid
			}
			obj, e := s.owned(userID, in.MediaID)
			if e != nil {
				return 0, false, nil, e
			}
			if obj.UploadStatus != StatusReady {
				return 0, false, nil, ErrMissingBlob
			}
			if obj.RecordID != "" && obj.RecordID != recordID {
				return 0, false, nil, ErrInUse
			}
			if obj.Type == TypePhoto {
				photoCount++
			}
			if obj.Type == TypeVoice {
				hasVoice = true
			}
			ids = append(ids, obj.ID)
		}
	}
	if photoCount > MaxPhotosPerRecord {
		return 0, false, nil, ErrTooLarge
	}
	return photoCount, hasVoice, ids, nil
}

func (s *Service) Bind(recordID string, ids []string) error {
	current, err := s.Store.ListByRecord(recordID)
	if err != nil {
		return err
	}
	keep := map[string]struct{}{}
	for _, id := range ids {
		keep[id] = struct{}{}
	}
	now := s.Now().UTC()
	for _, o := range current {
		if _, ok := keep[o.ID]; ok {
			continue
		}
		o.RecordID = ""
		o.UpdatedAt = now
		if err := s.Store.Update(o); err != nil {
			return err
		}
	}
	for _, id := range ids {
		o, err := s.Store.Get(id)
		if err != nil {
			return err
		}
		if o == nil {
			return ErrNotFound
		}
		o.RecordID = recordID
		o.UpdatedAt = now
		if err := s.Store.Update(*o); err != nil {
			return err
		}
	}
	return nil
}

func (s *Service) owned(userID, mediaID string) (*Object, error) {
	obj, err := s.Store.Get(mediaID)
	if err != nil {
		return nil, err
	}
	if obj == nil || obj.DeletedAt != nil || obj.UploadStatus == StatusDeleted {
		return nil, ErrNotFound
	}
	if obj.UserID != userID {
		return nil, ErrForbidden
	}
	return obj, nil
}
