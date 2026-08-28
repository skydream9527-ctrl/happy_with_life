package media

import (
	"os"
	"path/filepath"
	"strings"
)

// DiskBlob stores mock uploads on local disk so friend-beta ECS keeps photos
// across process restarts until a real OSS bucket is wired.
type DiskBlob struct {
	root string
}

func NewDiskBlob(root string) (*DiskBlob, error) {
	if root == "" {
		root = "data/media"
	}
	if err := os.MkdirAll(root, 0o750); err != nil {
		return nil, err
	}
	return &DiskBlob{root: root}, nil
}

func (d *DiskBlob) Provider() string { return "mock" }

func (d *DiskBlob) path(key string) (string, error) {
	if key == "" || strings.Contains(key, "..") || strings.HasPrefix(key, "/") {
		return "", ErrInvalid
	}
	return filepath.Join(d.root, filepath.FromSlash(key)), nil
}

func (d *DiskBlob) Put(mime string, key string, body []byte) error {
	p, err := d.path(key)
	if err != nil {
		return err
	}
	if err := os.MkdirAll(filepath.Dir(p), 0o750); err != nil {
		return err
	}
	if err := os.WriteFile(p, body, 0o640); err != nil {
		return err
	}
	_ = mime
	return nil
}

func (d *DiskBlob) Head(key string) (int64, string, error) {
	p, err := d.path(key)
	if err != nil {
		return 0, "", err
	}
	st, err := os.Stat(p)
	if err != nil {
		if os.IsNotExist(err) {
			return 0, "", ErrMissingBlob
		}
		return 0, "", err
	}
	return st.Size(), "", nil
}

func (d *DiskBlob) Get(key string) ([]byte, string, error) {
	p, err := d.path(key)
	if err != nil {
		return nil, "", err
	}
	b, err := os.ReadFile(p)
	if err != nil {
		if os.IsNotExist(err) {
			return nil, "", ErrMissingBlob
		}
		return nil, "", err
	}
	return b, "", nil
}

func (d *DiskBlob) Delete(key string) {
	p, err := d.path(key)
	if err != nil {
		return
	}
	_ = os.Remove(p)
}
