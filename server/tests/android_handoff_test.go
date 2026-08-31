package tests

import (
	"net/http"
	"testing"
)

func TestAndroidHandoffLoginRecordPull(t *testing.T) {
	ts, _ := testServer(t)
	code, body := postJSON(t, ts, "/api/v1/auth/sms/send", map[string]string{"phone": "13800138000"}, nil)
	if code != 200 {
		t.Fatalf("send %d %#v", code, body)
	}
	code, body = postJSON(t, ts, "/api/v1/auth/sms/verify", map[string]string{
		"phone": "13800138000", "code": "123456", "deviceId": "device-test-1", "platform": "android",
	}, nil)
	if code != 200 {
		t.Fatalf("verify %d %#v", code, body)
	}
	data := body["data"].(map[string]any)
	access := data["accessToken"].(string)
	refresh := data["refreshToken"].(string)
	authz := map[string]string{"Authorization": "Bearer " + access}

	code, body = doJSON(t, ts, http.MethodGet, "/api/v1/me", nil, authz)
	if code != 200 {
		t.Fatalf("me %d %#v", code, body)
	}
	spaceID := body["data"].(map[string]any)["personalSpaceId"].(string)

	code, body = postJSON(t, ts, "/api/v1/records", map[string]any{
		"spaceId": spaceID, "moodTag": "开心", "contentText": "联调第一条", "timezone": "Asia/Shanghai",
	}, mergeHeaders(authz, map[string]string{"Idempotency-Key": "01900000-0000-7000-8000-00000000abcd"}))
	if code != 200 {
		t.Fatalf("create %d %#v", code, body)
	}
	if body["data"].(map[string]any)["status"] != "APPLIED" {
		t.Fatalf("create status %#v", body)
	}

	code, body = doJSON(t, ts, http.MethodGet, "/api/v1/sync/pull?limit=50", nil, authz)
	if code != 200 {
		t.Fatalf("pull %d %#v", code, body)
	}

	code, body = postJSON(t, ts, "/api/v1/auth/token/refresh", map[string]string{
		"refreshToken": refresh, "deviceId": "device-test-1",
	}, nil)
	if code != 200 {
		t.Fatalf("refresh %d %#v", code, body)
	}
	access2 := body["data"].(map[string]any)["accessToken"].(string)
	newRefresh := body["data"].(map[string]any)["refreshToken"].(string)
	code, body = postJSON(t, ts, "/api/v1/auth/logout", map[string]string{
		"refreshToken": newRefresh,
	}, map[string]string{"Authorization": "Bearer " + access2})
	if code != 200 {
		t.Fatalf("logout %d %#v", code, body)
	}
}

func TestAndroidPasswordHandoff(t *testing.T) {
	ts, _ := testServer(t)
	code, body := postJSON(t, ts, "/api/v1/auth/register", map[string]string{
		"account": "handoff_user", "password": "pass1234", "deviceId": "device-test-1", "platform": "android",
	}, nil)
	if code != 200 {
		t.Fatalf("register %d %#v", code, body)
	}
	access := body["data"].(map[string]any)["accessToken"].(string)
	authz := map[string]string{"Authorization": "Bearer " + access}

	code, body = doJSON(t, ts, http.MethodGet, "/api/v1/me", nil, authz)
	if code != 200 {
		t.Fatalf("me %d %#v", code, body)
	}
	spaceID := body["data"].(map[string]any)["personalSpaceId"].(string)

	code, body = postJSON(t, ts, "/api/v1/sync/push", map[string]any{
		"batchId": "batch-1",
		"mutations": []map[string]any{{
			"mutationId":    "01900000-0000-7000-8000-00000000ab01",
			"entityType":    "RECORD",
			"operation":     "CREATE",
			"clientLocalId": 1,
			"payload": map[string]any{
				"spaceId": spaceID, "moodTag": "开心", "contentText": "密码联调", "timezone": "Asia/Shanghai",
			},
		}},
	}, authz)
	if code != 200 {
		t.Fatalf("push %d %#v", code, body)
	}

	code, body = doJSON(t, ts, http.MethodGet, "/api/v1/sync/pull?limit=20", nil, authz)
	if code != 200 {
		t.Fatalf("pull %d %#v", code, body)
	}
	changes, _ := body["data"].(map[string]any)["changes"].([]any)
	if len(changes) == 0 {
		t.Fatalf("pull empty %#v", body)
	}

	code, body = postJSON(t, ts, "/api/v1/auth/password/change", map[string]string{
		"oldPassword": "pass1234", "newPassword": "pass5678",
	}, authz)
	if code != 200 {
		t.Fatalf("change %d %#v", code, body)
	}

	code, body = postJSON(t, ts, "/api/v1/auth/login", map[string]string{
		"account": "handoff_user", "password": "pass5678", "deviceId": "device-test-1", "platform": "android",
	}, nil)
	if code != 200 {
		t.Fatalf("login %d %#v", code, body)
	}
}

func mergeHeaders(a, b map[string]string) map[string]string {
	out := map[string]string{}
	for k, v := range a {
		out[k] = v
	}
	for k, v := range b {
		out[k] = v
	}
	return out
}
