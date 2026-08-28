package media

import (
	"sync"
	"time"

	"github.com/skydream9527-ctrl/xiaoquexing-server/internal/id"
)

type Store interface {
	Insert(o Object) error
	Update(o Object) error
	Get(id string) (*Object, error)
	ListByRecord(recordID string) ([]Object, error)
	Usage(userID string) (int64, error)
}

type MemoryStore struct {
	mu   sync.Mutex
	byID map[string]Object
}

func NewMemoryStore() *MemoryStore {
	return &MemoryStore{byID: map[string]Object{}}
}

func (m *MemoryStore) Insert(o Object) error {
	m.mu.Lock()
	defer m.mu.Unlock()
	if o.ID == "" {
		o.ID = id.New()
	}
	m.byID[o.ID] = o
	return nil
}

func (m *MemoryStore) Update(o Object) error {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.byID[o.ID] = o
	return nil
}

func (m *MemoryStore) Get(id string) (*Object, error) {
	m.mu.Lock()
	defer m.mu.Unlock()
	o, ok := m.byID[id]
	if !ok {
		return nil, nil
	}
	cp := o
	return &cp, nil
}

func (m *MemoryStore) ListByRecord(recordID string) ([]Object, error) {
	m.mu.Lock()
	defer m.mu.Unlock()
	out := []Object{}
	for _, o := range m.byID {
		if o.RecordID == recordID && o.DeletedAt == nil && o.UploadStatus != StatusDeleted {
			out = append(out, o)
		}
	}
	return out, nil
}

func (m *MemoryStore) Usage(userID string) (int64, error) {
	m.mu.Lock()
	defer m.mu.Unlock()
	var n int64
	for _, o := range m.byID {
		if o.UserID != userID || o.DeletedAt != nil || o.UploadStatus == StatusDeleted {
			continue
		}
		if o.UploadStatus == StatusPending {
			n += o.ReservedBytes
		} else if o.UploadStatus == StatusReady {
			n += o.SizeBytes
		}
	}
	return n, nil
}

type MemoryBlob struct {
	mu   sync.Mutex
	data map[string]blob
}

type blob struct {
	MIME string
	Body []byte
}

func NewMemoryBlob() *MemoryBlob {
	return &MemoryBlob{data: map[string]blob{}}
}

func (b *MemoryBlob) Provider() string { return "mock" }

func (b *MemoryBlob) Put(mime string, key string, body []byte) error {
	b.mu.Lock()
	defer b.mu.Unlock()
	cp := make([]byte, len(body))
	copy(cp, body)
	b.data[key] = blob{MIME: mime, Body: cp}
	return nil
}

func (b *MemoryBlob) Head(key string) (int64, string, error) {
	b.mu.Lock()
	defer b.mu.Unlock()
	v, ok := b.data[key]
	if !ok {
		return 0, "", ErrMissingBlob
	}
	return int64(len(v.Body)), v.MIME, nil
}

func (b *MemoryBlob) Get(key string) ([]byte, string, error) {
	b.mu.Lock()
	defer b.mu.Unlock()
	v, ok := b.data[key]
	if !ok {
		return nil, "", ErrMissingBlob
	}
	cp := make([]byte, len(v.Body))
	copy(cp, v.Body)
	return cp, v.MIME, nil
}

func (b *MemoryBlob) Delete(key string) {
	b.mu.Lock()
	defer b.mu.Unlock()
	delete(b.data, key)
}

func NewID() string { return id.New() }

func nowUTC() time.Time { return time.Now().UTC() }
