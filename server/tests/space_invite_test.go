package tests

import (
	"net/http"
	"testing"
)

func TestSharedSpaceInviteAcceptAndPermissions(t *testing.T) {
	ts, _ := testServer(t)

	login := func(phone, device string) string {
		t.Helper()
		_, _ = postJSON(t, ts, "/api/v1/auth/sms/send", map[string]string{"phone": phone}, nil)
		code, body := postJSON(t, ts, "/api/v1/auth/sms/verify", map[string]string{
			"phone": phone, "code": "123456", "deviceId": device, "platform": "android",
		}, nil)
		if code != 200 {
			t.Fatalf("login %s %d %#v", phone, code, body)
		}
		return body["data"].(map[string]any)["accessToken"].(string)
	}

	a := login("13900001111", "dev-a")
	b := login("13900002222", "dev-b")
	authA := map[string]string{"Authorization": "Bearer " + a}
	authB := map[string]string{"Authorization": "Bearer " + b}

	code, body := postJSON(t, ts, "/api/v1/spaces", map[string]string{
		"name": "我们的小日子", "spaceType": "COUPLE", "plantType": "TREE",
	}, authA)
	if code != 201 && code != 200 {
		t.Fatalf("create space %d %#v", code, body)
	}
	spaceID := body["data"].(map[string]any)["id"].(string)

	code, body = postJSON(t, ts, "/api/v1/spaces/"+spaceID+"/invites", map[string]string{}, authA)
	if code != 200 {
		t.Fatalf("invite %d %#v", code, body)
	}
	token := body["data"].(map[string]any)["token"].(string)
	if token == "" {
		t.Fatal("missing token")
	}

	code, body = doJSON(t, ts, http.MethodGet, "/api/v1/invites/"+token, nil, nil)
	if code != 200 {
		t.Fatalf("peek %d %#v", code, body)
	}
	if body["data"].(map[string]any)["spaceName"] != "我们的小日子" {
		t.Fatalf("peek %#v", body)
	}

	code, body = postJSON(t, ts, "/api/v1/invites/accept", map[string]string{"token": token}, authB)
	if code != 200 {
		t.Fatalf("accept %d %#v", code, body)
	}

	code, body = doJSON(t, ts, http.MethodGet, "/api/v1/spaces/"+spaceID+"/members", nil, authB)
	if code != 200 {
		t.Fatalf("members %d %#v", code, body)
	}
	items := body["data"].(map[string]any)["items"].([]any)
	if len(items) != 2 {
		t.Fatalf("want 2 members got %d %#v", len(items), items)
	}

	code, body = postJSON(t, ts, "/api/v1/records", map[string]any{
		"spaceId": spaceID, "moodTag": "开心", "contentText": "一起吃饭", "timezone": "Asia/Shanghai",
	}, authB)
	if code != 200 {
		t.Fatalf("b record %d %#v", code, body)
	}
	recID := body["data"].(map[string]any)["serverId"].(string)

	code, _ = doJSON(t, ts, http.MethodDelete, "/api/v1/records/"+recID, nil, authA)
	if code != 403 {
		t.Fatalf("owner must not delete member record, got %d", code)
	}

	code, body = doJSON(t, ts, http.MethodGet, "/api/v1/spaces", nil, nil)
	// personal cannot invite
	code, body = doJSON(t, ts, http.MethodGet, "/api/v1/spaces", nil, authA)
	spaces := body["data"].(map[string]any)["items"].([]any)
	var personal string
	for _, it := range spaces {
		m := it.(map[string]any)
		if m["spaceType"] == "PERSONAL" {
			personal = m["id"].(string)
		}
	}
	code, body = postJSON(t, ts, "/api/v1/spaces/"+personal+"/invites", map[string]string{}, authA)
	if code != 400 {
		t.Fatalf("personal invite %d %#v", code, body)
	}

	code, body = postJSON(t, ts, "/api/v1/spaces/"+spaceID+"/leave", map[string]string{}, authA)
	if code != 400 {
		t.Fatalf("owner leave %d %#v", code, body)
	}

	code, body = postJSON(t, ts, "/api/v1/spaces/"+spaceID+"/leave", map[string]string{}, authB)
	if code != 200 {
		t.Fatalf("member leave %d %#v", code, body)
	}
}
