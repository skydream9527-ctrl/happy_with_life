package media

import (
	"errors"
	"time"
)

const (
	TypePhoto = "PHOTO"
	TypeVoice = "VOICE"

	StatusPending = "PENDING"
	StatusReady   = "READY"
	StatusMissing = "MISSING"
	StatusDeleted = "DELETED"

	DefaultQuotaBytes   int64 = 200 * 1024 * 1024
	DefaultMaxPhotoBytes int64 = 5 * 1024 * 1024
	MaxPhotosPerRecord        = 9
	STSExpire                 = 15 * time.Minute
	DownloadExpire            = 10 * time.Minute
)

var (
	ErrInvalid      = errors.New("media invalid")
	ErrQuota        = errors.New("media quota exceeded")
	ErrNotFound     = errors.New("media not found")
	ErrForbidden    = errors.New("media forbidden")
	ErrInUse        = errors.New("media in use")
	ErrMissingBlob  = errors.New("media blob missing")
	ErrTooLarge     = errors.New("media too large")
	ErrUnsupported  = errors.New("media type unsupported")
)

type Object struct {
	ID            string
	UserID        string
	RecordID      string
	Type          string
	ObjectKey     string
	UploadStatus  string
	MimeType      string
	SizeBytes     int64
	ReservedBytes int64
	SHA256        string
	Width         int
	Height        int
	DurationMS    int
	DeletedAt     *time.Time
	CreatedAt     time.Time
	UpdatedAt     time.Time
}

type STSRequest struct {
	Type     string
	MimeType string
	SizeBytes int64
	SHA256   string
	Width    int
	Height   int
}

type STSResult struct {
	MediaID   string
	ObjectKey string
	Method    string
	UploadURL string
	Headers   map[string]string
	ExpiresAt time.Time
	Provider  string
	QuotaUsed int64
	QuotaMax  int64
}

type Item struct {
	ID           string `json:"id"`
	Type         string `json:"type"`
	UploadStatus string `json:"uploadStatus"`
	MimeType     string `json:"mimeType"`
	SizeBytes    int64  `json:"sizeBytes"`
	Width        int    `json:"width,omitempty"`
	Height       int    `json:"height,omitempty"`
}

func (o Object) Item() Item {
	return Item{
		ID: o.ID, Type: o.Type, UploadStatus: o.UploadStatus,
		MimeType: o.MimeType, SizeBytes: o.SizeBytes, Width: o.Width, Height: o.Height,
	}
}

var photoMIME = map[string]string{
	"image/jpeg": ".jpg",
	"image/jpg":  ".jpg",
	"image/png":  ".png",
	"image/webp": ".webp",
	"image/heic": ".heic",
	"image/heif": ".heif",
}

var voiceMIME = map[string]string{
	"audio/mp4":  ".m4a",
	"audio/aac":  ".aac",
	"audio/mpeg": ".mp3",
	"audio/amr":  ".amr",
}

func ExtFor(mime string) (string, bool) {
	if e, ok := photoMIME[mime]; ok {
		return e, true
	}
	if e, ok := voiceMIME[mime]; ok {
		return e, true
	}
	return "", false
}
