package config

import (
	"fmt"
	"os"
	"strconv"
	"strings"
	"time"
)

const DomainRulesVersion = "1"

type Config struct {
	AppEnv             string
	AppVersion         string
	HTTPAddr           string
	LogLevel           string
	PostgresDSN        string
	RedisAddr          string
	RedisPassword      string
	RedisDB            int
	RedisKeyPrefix     string
	JWTSigningKey      string
	PhoneEncryptionKey []byte
	PhoneHashPepper    string
	SMSProvider        string
	SMSDevCode         string
	SMSCodeTTL         time.Duration
	AccessTokenTTL     time.Duration
	RefreshTokenTTL    time.Duration
	HTTPTimeout        time.Duration
	HTTPMaxBodyBytes   int64
	CORSOrigins        []string
	AllowInMemory      bool
	RunMigrations      bool
	AliyunSMSSignName  string
	AliyunSMSTemplate  string
	AliyunSMSEndpoint  string
	OSSProvider        string
	OSSBucket          string
	OSSEndpoint        string
	OSSAccessKeyID     string
	OSSAccessKeySecret string
	MediaDataDir       string
	PublicBaseURL      string
	MediaQuotaBytes    int64
	MaxPhotoBytes      int64
	AccountDeleteGrace time.Duration
}

func Load() (Config, error) {
	encHex := strings.TrimSpace(getenv("PHONE_ENCRYPTION_KEY", strings.Repeat("00", 32)))
	encKey, err := parseHexKey(encHex)
	if err != nil {
		return Config{}, fmt.Errorf("PHONE_ENCRYPTION_KEY: %w", err)
	}

	cfg := Config{
		AppEnv:             getenv("APP_ENV", "dev"),
		AppVersion:         getenv("APP_VERSION", "0.1.0"),
		HTTPAddr:           getenv("HTTP_ADDR", ":8080"),
		LogLevel:           getenv("LOG_LEVEL", "info"),
		PostgresDSN:        strings.TrimSpace(os.Getenv("POSTGRES_DSN")),
		RedisAddr:          getenv("REDIS_ADDR", "127.0.0.1:6379"),
		RedisPassword:      os.Getenv("REDIS_PASSWORD"),
		RedisDB:            getenvInt("REDIS_DB", 0),
		RedisKeyPrefix:     getenv("REDIS_KEY_PREFIX", "xqx:dev:"),
		JWTSigningKey:      getenv("JWT_SIGNING_KEY", "dev-only-change-me-32bytes-secret!!"),
		PhoneEncryptionKey: encKey,
		PhoneHashPepper:    getenv("PHONE_HASH_PEPPER", "dev-only-phone-pepper"),
		SMSProvider:        strings.ToLower(getenv("SMS_PROVIDER", "mock")),
		SMSDevCode:         getenv("SMS_DEV_CODE", "123456"),
		SMSCodeTTL:         getenvDuration("SMS_CODE_TTL", 5*time.Minute),
		AccessTokenTTL:     getenvDuration("ACCESS_TOKEN_TTL", 15*time.Minute),
		RefreshTokenTTL:    getenvDuration("REFRESH_TOKEN_TTL", 30*24*time.Hour),
		HTTPTimeout:        getenvDuration("HTTP_TIMEOUT", 15*time.Second),
		HTTPMaxBodyBytes:   int64(getenvInt("HTTP_MAX_BODY_BYTES", 1<<20)),
		CORSOrigins:        splitCSV(os.Getenv("CORS_ORIGINS")),
		AllowInMemory:      getenvBool("DEV_INMEMORY", false),
		RunMigrations:      getenvBool("RUN_MIGRATIONS", false),
		AliyunSMSSignName:  os.Getenv("ALIYUN_SMS_SIGN_NAME"),
		AliyunSMSTemplate:  os.Getenv("ALIYUN_SMS_TEMPLATE_CODE"),
		AliyunSMSEndpoint:  getenv("ALIYUN_SMS_ENDPOINT", "dysmsapi.aliyuncs.com"),
		OSSProvider:        strings.ToLower(getenv("OSS_PROVIDER", "mock")),
		OSSBucket:          os.Getenv("OSS_BUCKET"),
		OSSEndpoint:        getenv("OSS_ENDPOINT", "oss-cn-hangzhou.aliyuncs.com"),
		OSSAccessKeyID:     os.Getenv("OSS_ACCESS_KEY_ID"),
		OSSAccessKeySecret: os.Getenv("OSS_ACCESS_KEY_SECRET"),
		MediaDataDir:       getenv("MEDIA_DATA_DIR", "data/media"),
		PublicBaseURL:      strings.TrimRight(os.Getenv("PUBLIC_BASE_URL"), "/"),
		MediaQuotaBytes:    int64(getenvInt("MEDIA_QUOTA_BYTES", 200*1024*1024)),
		MaxPhotoBytes:      int64(getenvInt("MEDIA_MAX_PHOTO_BYTES", 5*1024*1024)),
		AccountDeleteGrace: getenvDuration("ACCOUNT_DELETE_GRACE", 24*time.Hour),
	}
	if err := cfg.Validate(); err != nil {
		return Config{}, err
	}
	return cfg, nil
}

func (c Config) Validate() error {
	if c.AppEnv == "" {
		return fmt.Errorf("APP_ENV is required")
	}
	if len(c.JWTSigningKey) < 32 {
		return fmt.Errorf("JWT_SIGNING_KEY must be at least 32 characters")
	}
	if len(c.PhoneEncryptionKey) != 32 {
		return fmt.Errorf("PHONE_ENCRYPTION_KEY must decode to 32 bytes")
	}
	if len(c.PhoneHashPepper) < 16 {
		return fmt.Errorf("PHONE_HASH_PEPPER must be at least 16 characters")
	}
	if c.SMSProvider != "mock" && c.SMSProvider != "aliyun" {
		return fmt.Errorf("SMS_PROVIDER must be mock or aliyun")
	}
	if c.OSSProvider != "mock" && c.OSSProvider != "aliyun" {
		return fmt.Errorf("OSS_PROVIDER must be mock or aliyun")
	}
	if c.MediaQuotaBytes <= 0 {
		return fmt.Errorf("MEDIA_QUOTA_BYTES must be positive")
	}
	if c.SMSDevCode == "" || len(c.SMSDevCode) < 4 {
		return fmt.Errorf("SMS_DEV_CODE is required for mock/dev verification")
	}
	prod := c.AppEnv == "prod" || c.AppEnv == "production" || c.AppEnv == "staging"
	if prod {
		if c.PostgresDSN == "" {
			return fmt.Errorf("POSTGRES_DSN is required in %s", c.AppEnv)
		}
		if c.AllowInMemory {
			return fmt.Errorf("DEV_INMEMORY is forbidden in %s", c.AppEnv)
		}
		if strings.Contains(c.JWTSigningKey, "dev-only") {
			return fmt.Errorf("JWT_SIGNING_KEY must not use the development default in %s", c.AppEnv)
		}
		if c.IsProd() && c.SMSProvider == "mock" {
			return fmt.Errorf("SMS_PROVIDER=mock is forbidden in prod")
		}
		if c.OSSProvider == "aliyun" && (c.OSSBucket == "" || c.OSSAccessKeyID == "" || c.OSSAccessKeySecret == "") {
			return fmt.Errorf("OSS_BUCKET and OSS_ACCESS_KEY_* are required when OSS_PROVIDER=aliyun")
		}
	}
	return nil
}

func (c Config) IsProd() bool {
	return c.AppEnv == "prod" || c.AppEnv == "production"
}

func getenv(key, def string) string {
	if v := strings.TrimSpace(os.Getenv(key)); v != "" {
		return v
	}
	return def
}

func getenvInt(key string, def int) int {
	v := strings.TrimSpace(os.Getenv(key))
	if v == "" {
		return def
	}
	n, err := strconv.Atoi(v)
	if err != nil {
		return def
	}
	return n
}

func getenvBool(key string, def bool) bool {
	v := strings.ToLower(strings.TrimSpace(os.Getenv(key)))
	if v == "" {
		return def
	}
	return v == "1" || v == "true" || v == "yes"
}

func getenvDuration(key string, def time.Duration) time.Duration {
	v := strings.TrimSpace(os.Getenv(key))
	if v == "" {
		return def
	}
	d, err := time.ParseDuration(v)
	if err != nil {
		return def
	}
	return d
}

func splitCSV(s string) []string {
	if strings.TrimSpace(s) == "" {
		return nil
	}
	parts := strings.Split(s, ",")
	out := make([]string, 0, len(parts))
	for _, p := range parts {
		p = strings.TrimSpace(p)
		if p != "" {
			out = append(out, p)
		}
	}
	return out
}

func parseHexKey(s string) ([]byte, error) {
	s = strings.TrimPrefix(strings.ToLower(s), "0x")
	if len(s) != 64 {
		return nil, fmt.Errorf("expected 64 hex chars, got %d", len(s))
	}
	out := make([]byte, 32)
	for i := 0; i < 32; i++ {
		a := unhex(s[i*2])
		b := unhex(s[i*2+1])
		if a < 0 || b < 0 {
			return nil, fmt.Errorf("invalid hex")
		}
		out[i] = byte(a<<4 | b)
	}
	return out, nil
}

func unhex(c byte) int {
	switch {
	case c >= '0' && c <= '9':
		return int(c - '0')
	case c >= 'a' && c <= 'f':
		return int(c - 'a' + 10)
	default:
		return -1
	}
}
