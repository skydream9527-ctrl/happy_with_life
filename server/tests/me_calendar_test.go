package tests

import (
	"net/http"
	"testing"
)

func TestMeAndCalendar(t *testing.T) {
	ts, _ := testServer(t)
	_, _ = postJSON(t, ts, "/api/v1/auth/sms/send", map[string]string{"phone": "13600003333"}, nil)
	code, body := postJSON(t, ts, "/api/v1/auth/sms/verify", map[string]string{
		"phone": "13600003333", "code": "123456", "deviceId": "device-test-1",
	}, nil)
	if code != 200 {
		t.Fatalf("verify %d %#v", code, body)
	}
	if body["data"].(map[string]any)["displayName"] != "旅行者" {
		t.Fatalf("displayName %#v", body["data"])
	}
	access := body["data"].(map[string]any)["accessToken"].(string)
	authz := map[string]string{"Authorization": "Bearer " + access}

	code, body = doJSON(t, ts, http.MethodGet, "/api/v1/me", nil, authz)
	if code != 200 {
		t.Fatalf("me %d %#v", code, body)
	}
	me := body["data"].(map[string]any)
	if me["maskedPhone"] != "136****3333" {
		t.Fatalf("mask %#v", me)
	}
	spaceID := me["personalSpaceId"].(string)
	if spaceID == "" {
		t.Fatal("personalSpaceId empty")
	}

	code, body = doJSON(t, ts, http.MethodPatch, "/api/v1/me", map[string]string{"displayName": "小确幸用户"}, authz)
	if code != 200 {
		t.Fatalf("patch me %d %#v", code, body)
	}
	if body["data"].(map[string]any)["displayName"] != "小确幸用户" {
		t.Fatalf("renamed %#v", body)
	}

	_, _ = postJSON(t, ts, "/api/v1/records", map[string]any{
		"spaceId": spaceID, "moodTag": "开心", "contentText": "日历", "timezone": "Asia/Shanghai",
	}, authz)
	code, body = doJSON(t, ts, http.MethodGet, "/api/v1/stats/calendar?spaceId="+spaceID, nil, authz)
	if code != 200 {
		t.Fatalf("cal %d %#v", code, body)
	}
	days := body["data"].(map[string]any)["days"].([]any)
	if len(days) < 1 {
		t.Fatalf("days %#v", body)
	}
	if days[0].(map[string]any)["moodTag"] != "开心" {
		t.Fatalf("mood %#v", days[0])
	}
}
