package tests

import (
	"net/http"
	"testing"
)

func TestRecordSyncAndGP(t *testing.T) {
	ts, _ := testServer(t)
	_, _ = postJSON(t, ts, "/api/v1/auth/sms/send", map[string]string{"phone": "13600001111"}, nil)
	code, body := postJSON(t, ts, "/api/v1/auth/sms/verify", map[string]string{
		"phone": "13600001111", "code": "123456", "deviceId": "device-test-1", "platform": "android",
	}, nil)
	if code != 200 {
		t.Fatalf("verify %d %#v", code, body)
	}
	access := body["data"].(map[string]any)["accessToken"].(string)
	authz := map[string]string{"Authorization": "Bearer " + access}

	code, body = doJSON(t, ts, http.MethodGet, "/api/v1/spaces", nil, authz)
	if code != 200 {
		t.Fatalf("spaces %d %#v", code, body)
	}
	items := body["data"].(map[string]any)["items"].([]any)
	if len(items) != 1 {
		t.Fatalf("want 1 personal space got %#v", items)
	}
	spaceID := items[0].(map[string]any)["id"].(string)

	code, body = postJSON(t, ts, "/api/v1/records", map[string]any{
		"spaceId": spaceID, "moodTag": "开心", "contentText": "今天阳光很好", "timezone": "Asia/Shanghai",
	}, authz)
	if code != 200 {
		t.Fatalf("create %d %#v", code, body)
	}
	res := body["data"].(map[string]any)
	if res["status"] != "APPLIED" {
		t.Fatalf("status %#v", res)
	}
	if res["authoritative"].(map[string]any)["gpFinal"].(float64) < 10 {
		t.Fatalf("gp %#v", res["authoritative"])
	}
	serverID := res["serverId"].(string)
	version := int64(res["version"].(float64))

	mutID := "01900000-0000-7000-8000-000000000001"
	pushBody := map[string]any{
		"batchId": "batch-1",
		"mutations": []map[string]any{{
			"mutationId": mutID, "entityType": "RECORD", "operation": "UPSERT",
			"clientLocalId": 7, "baseVersion": 0, "timezone": "Asia/Shanghai",
			"payload": map[string]any{"spaceId": spaceID, "moodTag": "平静", "contentText": "第二笔"},
		}},
	}
	code, body = postJSON(t, ts, "/api/v1/sync/push", pushBody, authz)
	if code != 200 {
		t.Fatalf("push %d %#v", code, body)
	}
	if body["data"].(map[string]any)["results"].([]any)[0].(map[string]any)["status"] != "APPLIED" {
		t.Fatalf("push %#v", body)
	}
	code, body = postJSON(t, ts, "/api/v1/sync/push", pushBody, authz)
	if code != 200 {
		t.Fatalf("dup %d %#v", code, body)
	}
	if body["data"].(map[string]any)["results"].([]any)[0].(map[string]any)["status"] != "DUPLICATE" {
		t.Fatalf("want DUPLICATE %#v", body)
	}
	pushBody["mutations"].([]map[string]any)[0]["payload"] = map[string]any{
		"spaceId": spaceID, "moodTag": "兴奋", "contentText": "不同hash",
	}
	code, body = postJSON(t, ts, "/api/v1/sync/push", pushBody, authz)
	if code != 200 {
		t.Fatalf("reuse %d %#v", code, body)
	}
	errObj := body["data"].(map[string]any)["results"].([]any)[0].(map[string]any)["error"].(map[string]any)
	if errObj["code"] != "MUTATION_ID_REUSED" {
		t.Fatalf("want reused %#v", body)
	}

	code, _ = doJSON(t, ts, http.MethodPatch, "/api/v1/records/"+serverID, map[string]any{
		"moodTag": "感动", "contentText": "改一下", "baseVersion": 0, "spaceId": spaceID,
	}, authz)
	if code != 409 {
		t.Fatalf("stale patch want 409 got %d", code)
	}
	code, _ = doJSON(t, ts, http.MethodPatch, "/api/v1/records/"+serverID, map[string]any{
		"moodTag": "感动", "contentText": "改一下", "baseVersion": version, "spaceId": spaceID,
	}, authz)
	if code != 200 {
		t.Fatalf("patch %d", code)
	}
	code, _ = doJSON(t, ts, http.MethodDelete, "/api/v1/records/"+serverID, map[string]any{"baseVersion": 2}, authz)
	if code != 200 {
		t.Fatalf("delete %d", code)
	}

	code, body = doJSON(t, ts, http.MethodGet, "/api/v1/sync/pull?limit=50", nil, authz)
	if code != 200 {
		t.Fatalf("pull %d %#v", code, body)
	}
	changes := body["data"].(map[string]any)["changes"].([]any)
	if len(changes) < 2 {
		t.Fatalf("pull %#v", changes)
	}
	code, body = doJSON(t, ts, http.MethodGet, "/api/v1/spaces/"+spaceID+"/plant", nil, authz)
	if code != 200 {
		t.Fatalf("plant %d %#v", code, body)
	}
}

func TestMoodRequiredAndForbidden(t *testing.T) {
	ts, _ := testServer(t)
	_, _ = postJSON(t, ts, "/api/v1/auth/sms/send", map[string]string{"phone": "13600002222"}, nil)
	_, body := postJSON(t, ts, "/api/v1/auth/sms/verify", map[string]string{
		"phone": "13600002222", "code": "123456", "deviceId": "device-test-1",
	}, nil)
	access := body["data"].(map[string]any)["accessToken"].(string)
	authz := map[string]string{"Authorization": "Bearer " + access}
	_, spaces := doJSON(t, ts, http.MethodGet, "/api/v1/spaces", nil, authz)
	spaceID := spaces["data"].(map[string]any)["items"].([]any)[0].(map[string]any)["id"].(string)
	code, body := postJSON(t, ts, "/api/v1/records", map[string]any{
		"spaceId": spaceID, "contentText": "没有心情",
	}, authz)
	if code != 400 {
		t.Fatalf("mood required want 400 got %d %#v", code, body)
	}
	code, _ = doJSON(t, ts, http.MethodGet, "/api/v1/spaces/00000000-0000-7000-8000-000000000099", nil, authz)
	if code != 404 && code != 400 {
		t.Fatalf("missing space %d", code)
	}
}
