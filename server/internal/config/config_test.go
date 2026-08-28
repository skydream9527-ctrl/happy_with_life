package config

import "testing"

func TestValidateRejectsShortJWT(t *testing.T) {
	t.Setenv("APP_ENV", "dev")
	t.Setenv("JWT_SIGNING_KEY", "short")
	t.Setenv("PHONE_ENCRYPTION_KEY", "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
	t.Setenv("PHONE_HASH_PEPPER", "sixteen-chars-ok")
	if _, err := Load(); err == nil {
		t.Fatal("expected error")
	}
}

func TestProdForbidsInMemory(t *testing.T) {
	t.Setenv("APP_ENV", "prod")
	t.Setenv("POSTGRES_DSN", "postgres://xqx:xqx@localhost:5432/xiaoquexing?sslmode=disable")
	t.Setenv("JWT_SIGNING_KEY", "production-signing-key-32bytes-min")
	t.Setenv("PHONE_ENCRYPTION_KEY", "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
	t.Setenv("PHONE_HASH_PEPPER", "sixteen-chars-ok")
	t.Setenv("DEV_INMEMORY", "true")
	if _, err := Load(); err == nil {
		t.Fatal("expected error")
	}
}

func TestProdForbidsMockSMS(t *testing.T) {
	t.Setenv("APP_ENV", "prod")
	t.Setenv("POSTGRES_DSN", "postgres://xqx:xqx@localhost:5432/xiaoquexing?sslmode=disable")
	t.Setenv("JWT_SIGNING_KEY", "production-signing-key-32bytes-min")
	t.Setenv("PHONE_ENCRYPTION_KEY", "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
	t.Setenv("PHONE_HASH_PEPPER", "sixteen-chars-ok")
	t.Setenv("SMS_PROVIDER", "mock")
	if _, err := Load(); err == nil {
		t.Fatal("expected mock forbidden in prod")
	}
}

func TestDevLoad(t *testing.T) {
	t.Setenv("APP_ENV", "dev")
	t.Setenv("JWT_SIGNING_KEY", "dev-only-change-me-32bytes-secret!!")
	t.Setenv("PHONE_ENCRYPTION_KEY", "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
	t.Setenv("PHONE_HASH_PEPPER", "sixteen-chars-ok")
	t.Setenv("DEV_INMEMORY", "true")
	cfg, err := Load()
	if err != nil {
		t.Fatal(err)
	}
	if !cfg.AllowInMemory {
		t.Fatal("expected in-memory")
	}
}
