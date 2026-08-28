package tests

import (
	"bytes"
	"net/http"
	"testing"
)

func TestPhotoSTSUploadAndRecordGP(t *testing.T) {
	ts, _ := testServer(t)
	_, _ = postJSON(t, ts, "/api/v1/auth/sms/send", map[string]string{"phone": "13700001111"}, nil)
	code, body := postJSON(t, ts, "/api/v1/auth/sms/verify", map[string]string{
		"phone": "13700001111", "code": "123456", "deviceId": "device-media-1", "platform": "android",
	}, nil)
	if code != 200 {
		t.Fatalf("verify %d %#v", code, body)
	}
	access := body["data"].(map[string]any)["accessToken"].(string)
	authz := map[string]string{"Authorization": "Bearer " + access}

	code, body = doJSON(t, ts, http.MethodGet, "/api/v1/spaces", nil, authz)
	spaceID := body["data"].(map[string]any)["items"].([]any)[0].(map[string]any)["id"].(string)

	jpeg := []byte("\xff\xd8\xff\xd9fakejpeg")
	code, body = postJSON(t, ts, "/api/v1/media/sts", map[string]any{
		"type": "PHOTO", "mimeType": "image/jpeg", "sizeBytes": len(jpeg),
	}, authz)
	if code != 200 {
		t.Fatalf("sts %d %#v", code, body)
	}
	data := body["data"].(map[string]any)
	mediaID := data["mediaId"].(string)
	uploadURL := data["uploadUrl"].(string)
	if uploadURL == "" {
		t.Fatal("missing uploadUrl")
	}
	if uploadURL[0] == '/' {
		uploadURL = ts.URL + uploadURL
	}

	req, _ := http.NewRequest(http.MethodPut, uploadURL, bytes.NewReader(jpeg))
	req.Header.Set("Content-Type", "image/jpeg")
	res, err := http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	res.Body.Close()
	if res.StatusCode != 200 {
		t.Fatalf("put %d", res.StatusCode)
	}

	code, body = postJSON(t, ts, "/api/v1/media/complete", map[string]string{"mediaId": mediaID}, authz)
	if code != 200 {
		t.Fatalf("complete %d %#v", code, body)
	}
	if body["data"].(map[string]any)["uploadStatus"] != "READY" {
		t.Fatalf("status %#v", body)
	}

	code, body = postJSON(t, ts, "/api/v1/records", map[string]any{
		"spaceId": spaceID, "moodTag": "开心", "contentText": "拍了一张", "timezone": "Asia/Shanghai",
		"media": []map[string]string{{"mediaId": mediaID, "type": "PHOTO"}},
	}, authz)
	if code != 200 {
		t.Fatalf("record %d %#v", code, body)
	}
	gp := body["data"].(map[string]any)["authoritative"].(map[string]any)["gpFinal"].(float64)
	if gp < 13 {
		t.Fatalf("photo should add GP, got %v", gp)
	}

	code, body = doJSON(t, ts, http.MethodGet, "/api/v1/media/"+mediaID+"/download-url", nil, authz)
	if code != 200 {
		t.Fatalf("download-url %d %#v", code, body)
	}
	getURL := body["data"].(map[string]any)["url"].(string)
	if getURL[0] == '/' {
		getURL = ts.URL + getURL
	}
	gres, err := http.Get(getURL)
	if err != nil {
		t.Fatal(err)
	}
	defer gres.Body.Close()
	if gres.StatusCode != 200 {
		t.Fatalf("content %d", gres.StatusCode)
	}

	code, body = doJSON(t, ts, http.MethodDelete, "/api/v1/media/"+mediaID, nil, authz)
	if code != 409 {
		t.Fatalf("want in-use 409 got %d %#v", code, body)
	}
}

func TestMediaQuotaAndFakePhotoRejected(t *testing.T) {
	ts, _ := testServer(t)
	_, _ = postJSON(t, ts, "/api/v1/auth/sms/send", map[string]string{"phone": "13700002222"}, nil)
	_, body := postJSON(t, ts, "/api/v1/auth/sms/verify", map[string]string{
		"phone": "13700002222", "code": "123456", "deviceId": "device-media-2",
	}, nil)
	access := body["data"].(map[string]any)["accessToken"].(string)
	authz := map[string]string{"Authorization": "Bearer " + access}
	_, body = doJSON(t, ts, http.MethodGet, "/api/v1/spaces", nil, authz)
	spaceID := body["data"].(map[string]any)["items"].([]any)[0].(map[string]any)["id"].(string)

	code, body := postJSON(t, ts, "/api/v1/records", map[string]any{
		"spaceId": spaceID, "moodTag": "开心", "contentText": "假装有图",
		"media": []map[string]string{{"type": "PHOTO"}},
	}, authz)
	if code != 400 {
		t.Fatalf("fake photo should fail %d %#v", code, body)
	}

	code, body = postJSON(t, ts, "/api/v1/media/sts", map[string]any{
		"type": "PHOTO", "mimeType": "image/jpeg", "sizeBytes": 200*1024*1024 + 1,
	}, authz)
	if code != 400 && code != 403 {
		t.Fatalf("oversize sts %d %#v", code, body)
	}

	code, body = postJSON(t, ts, "/api/v1/media/sts", map[string]any{
		"type": "PHOTO", "mimeType": "application/pdf", "sizeBytes": 100,
	}, authz)
	if code != 400 {
		t.Fatalf("pdf sts %d %#v", code, body)
	}
}
