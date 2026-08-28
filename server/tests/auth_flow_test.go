package tests

import (
	"bytes"
	"context"
	"encoding/json"
	"io"
	"log/slog"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/alicebob/miniredis/v2"
	goredis "github.com/redis/go-redis/v9"
	"github.com/skydream9527-ctrl/xiaoquexing-server/internal/auth"
	"github.com/skydream9527-ctrl/xiaoquexing-server/internal/config"
	"github.com/skydream9527-ctrl/xiaoquexing-server/internal/ledger"
	"github.com/skydream9527-ctrl/xiaoquexing-server/internal/platform/aliyun"
	redisx "github.com/skydream9527-ctrl/xiaoquexing-server/internal/platform/redis"
	httpx "github.com/skydream9527-ctrl/xiaoquexing-server/internal/transport/http"
)

func testServer(t *testing.T) (*httptest.Server, *auth.Service) {
	t.Helper()
	mr, err := miniredis.Run()
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(mr.Close)
	rdb := redisx.NewWith(goredis.NewClient(&goredis.Options{Addr: mr.Addr()}), "xqx:test:")
	key := bytes.Repeat([]byte{7}, 32)
	cfg := config.Config{
		AppEnv: "test", AppVersion: "test", HTTPAddr: ":0",
		JWTSigningKey:      "test-signing-key-32-bytes-min!!",
		PhoneEncryptionKey: key, PhoneHashPepper: "test-phone-pepper-xx",
		SMSProvider: "mock", SMSDevCode: "123456",
		SMSCodeTTL: 5 * time.Minute, AccessTokenTTL: 15 * time.Minute, RefreshTokenTTL: 30 * 24 * time.Hour,
		HTTPMaxBodyBytes: 1 << 20,
	}
	store := auth.NewMemoryStore()
	led := ledger.NewService(ledger.NewMemory())
	svc := auth.NewService(cfg, store, rdb, aliyun.MockSMS{Log: slog.Default()}, slog.Default())
	svc.Bootstrap = led
	engine := httpx.NewRouter(cfg, slog.Default(), httpx.Deps{
		StoreMode:  "memory",
		Auth:       svc,
		Ledger:     led,
		ReadyRedis: func(ctx context.Context) error { return rdb.Ping(ctx) },
	})
	ts := httptest.NewServer(engine)
	t.Cleanup(ts.Close)
	return ts, svc
}

func postJSON(t *testing.T, ts *httptest.Server, path string, body any, headers map[string]string) (int, map[string]any) {
	t.Helper()
	b, _ := json.Marshal(body)
	req, _ := http.NewRequest(http.MethodPost, ts.URL+path, bytes.NewReader(b))
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("X-Device-ID", "device-test-1")
	for k, v := range headers {
		req.Header.Set(k, v)
	}
	res, err := http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	defer res.Body.Close()
	raw, _ := io.ReadAll(res.Body)
	var out map[string]any
	_ = json.Unmarshal(raw, &out)
	return res.StatusCode, out
}

func TestHealthLive(t *testing.T) {
	ts, _ := testServer(t)
	res, err := http.Get(ts.URL + "/health/live")
	if err != nil {
		t.Fatal(err)
	}
	defer res.Body.Close()
	if res.StatusCode != 200 {
		t.Fatalf("status %d", res.StatusCode)
	}
	if res.Header.Get("X-Request-ID") == "" {
		t.Fatal("missing request id")
	}
}

func TestMeta(t *testing.T) {
	ts, _ := testServer(t)
	res, err := http.Get(ts.URL + "/api/v1/meta")
	if err != nil {
		t.Fatal(err)
	}
	defer res.Body.Close()
	if res.StatusCode != 200 {
		t.Fatalf("status %d", res.StatusCode)
	}
}

func TestMockSMSLoginRefreshLogoutAndReuse(t *testing.T) {
	ts, _ := testServer(t)
	code, body := postJSON(t, ts, "/api/v1/auth/sms/send", map[string]string{"phone": "13812345678"}, nil)
	if code != 200 {
		t.Fatalf("send %d %#v", code, body)
	}
	if _, has := body["data"].(map[string]any)["accepted"]; !has {
		t.Fatalf("send body %#v", body)
	}
	code, body = postJSON(t, ts, "/api/v1/auth/sms/verify", map[string]string{
		"phone": "13812345678", "code": "000000",
	}, nil)
	if code != 401 {
		t.Fatalf("bad code should 401, got %d", code)
	}
	code, body = postJSON(t, ts, "/api/v1/auth/sms/verify", map[string]string{
		"phone": "13812345678", "code": "123456", "deviceId": "device-test-1", "platform": "android",
	}, nil)
	if code != 200 {
		t.Fatalf("verify %d %#v", code, body)
	}
	data := body["data"].(map[string]any)
	access := data["accessToken"].(string)
	refresh := data["refreshToken"].(string)
	if access == "" || refresh == "" {
		t.Fatal("missing tokens")
	}

	code, body = postJSON(t, ts, "/api/v1/auth/token/refresh", map[string]string{"refreshToken": refresh, "deviceId": "device-test-1"}, nil)
	if code != 200 {
		t.Fatalf("refresh %d %#v", code, body)
	}
	data = body["data"].(map[string]any)
	refresh2 := data["refreshToken"].(string)
	if refresh2 == refresh {
		t.Fatal("refresh token must rotate")
	}

	code, body = postJSON(t, ts, "/api/v1/auth/token/refresh", map[string]string{"refreshToken": refresh}, nil)
	if code != 401 {
		t.Fatalf("reuse want 401 got %d %#v", code, body)
	}
	errObj := body["error"].(map[string]any)
	if errObj["code"] != "REFRESH_REUSED" {
		t.Fatalf("want REFRESH_REUSED got %#v", errObj)
	}

	code, body = postJSON(t, ts, "/api/v1/auth/token/refresh", map[string]string{"refreshToken": refresh2}, nil)
	if code != 401 {
		t.Fatalf("family revoke want 401 got %d %#v", code, body)
	}

	code, _ = postJSON(t, ts, "/api/v1/auth/sms/verify", map[string]string{
		"phone": "13812345678", "code": "123456", "deviceId": "device-test-1",
	}, nil)
	if code != 401 {
		t.Fatalf("code consumed, want 401 got %d", code)
	}
}

func TestSendDoesNotRevealAccount(t *testing.T) {
	ts, _ := testServer(t)
	_, a := postJSON(t, ts, "/api/v1/auth/sms/send", map[string]string{"phone": "13900000001"}, nil)
	_, b := postJSON(t, ts, "/api/v1/auth/sms/send", map[string]string{"phone": "13900000002"}, nil)
	if a["data"].(map[string]any)["accepted"] != b["data"].(map[string]any)["accepted"] {
		t.Fatal("responses must not leak registration")
	}
}

func TestLogoutAndAccountDelete(t *testing.T) {
	ts, svc := testServer(t)
	_, _ = postJSON(t, ts, "/api/v1/auth/sms/send", map[string]string{"phone": "13700000000"}, nil)
	_, body := postJSON(t, ts, "/api/v1/auth/sms/verify", map[string]string{
		"phone": "13700000000", "code": "123456", "deviceId": "dev-2",
	}, nil)
	data := body["data"].(map[string]any)
	access := data["accessToken"].(string)
	refresh := data["refreshToken"].(string)
	code, _ := postJSON(t, ts, "/api/v1/auth/logout", map[string]string{"refreshToken": refresh}, map[string]string{
		"Authorization": "Bearer " + access,
	})
	if code != 200 {
		t.Fatalf("logout %d", code)
	}
	code, _ = postJSON(t, ts, "/api/v1/auth/token/refresh", map[string]string{"refreshToken": refresh}, nil)
	if code != 401 {
		t.Fatalf("logged out refresh %d", code)
	}
	if err := svc.SendSMS(context.Background(), "13700000000", "dev-2", "127.0.0.1"); err != nil {
		t.Fatal(err)
	}
	_, body = postJSON(t, ts, "/api/v1/auth/sms/verify", map[string]string{
		"phone": "13700000000", "code": "123456", "deviceId": "dev-2",
	}, nil)
	access = body["data"].(map[string]any)["accessToken"].(string)
	req, _ := http.NewRequest(http.MethodDelete, ts.URL+"/api/v1/account", nil)
	req.Header.Set("Authorization", "Bearer "+access)
	res, err := http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	defer res.Body.Close()
	if res.StatusCode != 200 {
		t.Fatalf("delete %d", res.StatusCode)
	}
}
