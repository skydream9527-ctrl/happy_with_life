package aliyun

import (
	"crypto/hmac"
	"crypto/sha1"
	"encoding/base64"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"strconv"
	"strings"
	"time"

	"github.com/skydream9527-ctrl/xiaoquexing-server/internal/media"
)

// OSS issues V1 presigned PUT/GET/HEAD URLs. The Android client never sees the AK.
type OSS struct {
	Bucket          string
	Endpoint        string
	AccessKeyID     string
	AccessKeySecret string
	HTTP            *http.Client
}

func (o OSS) Provider() string { return "aliyun" }

func (o OSS) configured() bool {
	return o.Bucket != "" && o.Endpoint != "" && o.AccessKeyID != "" && o.AccessKeySecret != ""
}

func (o OSS) Presign(verb, key, contentType string, exp time.Time) (string, error) {
	if !o.configured() {
		return "", media.ErrInvalid
	}
	expires := strconv.FormatInt(exp.Unix(), 10)
	resource := "/" + o.Bucket + "/" + strings.TrimPrefix(key, "/")
	stringToSign := verb + "\n\n" + contentType + "\n" + expires + "\n" + resource
	mac := hmac.New(sha1.New, []byte(o.AccessKeySecret))
	_, _ = mac.Write([]byte(stringToSign))
	sig := base64.StdEncoding.EncodeToString(mac.Sum(nil))
	host := o.Bucket + "." + strings.TrimPrefix(o.Endpoint, "https://")
	host = strings.TrimPrefix(host, "http://")
	u := &url.URL{Scheme: "https", Host: host, Path: "/" + strings.TrimPrefix(key, "/")}
	q := u.Query()
	q.Set("OSSAccessKeyId", o.AccessKeyID)
	q.Set("Expires", expires)
	q.Set("Signature", sig)
	u.RawQuery = q.Encode()
	return u.String(), nil
}

func (o OSS) Put(mime, key string, _ []byte) error {
	_, _ = mime, key
	return fmt.Errorf("aliyun oss does not accept server-side put")
}

func (o OSS) Head(key string) (int64, string, error) {
	if o.HTTP == nil {
		o.HTTP = &http.Client{Timeout: 10 * time.Second}
	}
	u, err := o.Presign(http.MethodHead, key, "", time.Now().UTC().Add(2*time.Minute))
	if err != nil {
		return 0, "", err
	}
	req, err := http.NewRequest(http.MethodHead, u, nil)
	if err != nil {
		return 0, "", err
	}
	res, err := o.HTTP.Do(req)
	if err != nil {
		return 0, "", err
	}
	defer res.Body.Close()
	_, _ = io.Copy(io.Discard, res.Body)
	if res.StatusCode == http.StatusNotFound {
		return 0, "", media.ErrMissingBlob
	}
	if res.StatusCode >= 300 {
		return 0, "", fmt.Errorf("oss head %d", res.StatusCode)
	}
	n := res.ContentLength
	if n < 0 {
		n = 0
	}
	return n, res.Header.Get("Content-Type"), nil
}

func (o OSS) Get(key string) ([]byte, string, error) {
	_ = key
	return nil, "", fmt.Errorf("aliyun oss get goes through presigned url")
}

func (o OSS) Delete(key string) { _ = key }
