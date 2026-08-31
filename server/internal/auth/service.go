package auth

import (
	"context"
	"crypto/sha256"
	"encoding/hex"
	"errors"
	"log/slog"
	"strings"
	"time"
	"unicode/utf8"

	"github.com/skydream9527-ctrl/xiaoquexing-server/internal/config"
	"github.com/skydream9527-ctrl/xiaoquexing-server/internal/id"
	"github.com/skydream9527-ctrl/xiaoquexing-server/internal/platform/aliyun"
	redisx "github.com/skydream9527-ctrl/xiaoquexing-server/internal/platform/redis"
)

var (
	ErrInvalidPhone   = errors.New("invalid phone")
	ErrInvalidName    = errors.New("invalid name")
	ErrRateLimited    = errors.New("rate limited")
	ErrInvalidCode    = errors.New("invalid code")
	ErrSMSUnavailable = errors.New("sms unavailable")
	ErrUnauthorized   = errors.New("unauthorized")
	ErrTokenExpired   = errors.New("token expired")
	ErrRefreshReused  = errors.New("refresh reused")
)

type SpaceBootstrap interface {
	EnsurePersonalSpace(userID string) (string, error)
}

type Service struct {
	cfg       config.Config
	store     Store
	redis     *redisx.Client
	sms       aliyun.SMSProvider
	log       *slog.Logger
	now       func() time.Time
	RetrySec  int
	Bootstrap SpaceBootstrap
}

func NewService(cfg config.Config, store Store, redis *redisx.Client, sms aliyun.SMSProvider, log *slog.Logger) *Service {
	return &Service{cfg: cfg, store: store, redis: redis, sms: sms, log: log, now: time.Now, RetrySec: 60}
}

type TokenPair struct {
	AccessToken  string `json:"accessToken"`
	RefreshToken string `json:"refreshToken"`
	TokenType    string `json:"tokenType"`
	ExpiresIn    int    `json:"expiresIn"`
	UserID       string `json:"userId"`
	DeviceID     string `json:"deviceId"`
	DisplayName  string `json:"displayName,omitempty"`
}

type Profile struct {
	UserID          string `json:"userId"`
	DisplayName     string `json:"displayName"`
	Status          string `json:"status"`
	MaskedPhone     string `json:"maskedPhone"`
	PersonalSpaceID string `json:"personalSpaceId"`
	CreatedAt       string `json:"createdAt"`
}

func (s *Service) SendSMS(ctx context.Context, rawPhone, deviceID, ip string) error {
	e164, err := NormalizePhone(rawPhone)
	if err != nil {
		return ErrInvalidPhone
	}
	if s.redis == nil {
		return ErrSMSUnavailable
	}
	phoneHash := HashPhone(e164, s.cfg.PhoneHashPepper)
	if err := s.enforceLimits(ctx, phoneHash, deviceID, ip); err != nil {
		return err
	}
	code := s.cfg.SMSDevCode
	if s.cfg.SMSProvider != "mock" {
		raw, err := RandomToken()
		if err != nil {
			return err
		}
		code = raw[:6]
		n := 0
		for _, c := range code {
			n = n*10 + int(c-'0')%10
		}
		code = numericCode(raw)
	}
	sum := sha256.Sum256([]byte(code + ":" + phoneHash))
	if err := s.redis.SetCodeHash(ctx, phoneHash, hex.EncodeToString(sum[:]), s.cfg.SMSCodeTTL); err != nil {
		s.log.Error("store sms hash failed", "err", err)
		return ErrSMSUnavailable
	}
	if _, err := s.sms.Send(ctx, e164, code); err != nil {
		s.log.Warn("sms provider failed", "provider", s.sms.Name(), "err", err)
		return ErrSMSUnavailable
	}
	return nil
}

func numericCode(hexTok string) string {
	var b strings.Builder
	for _, c := range hexTok {
		if c >= '0' && c <= '9' {
			b.WriteRune(c)
		}
		if b.Len() == 6 {
			break
		}
	}
	for b.Len() < 6 {
		b.WriteByte('0')
	}
	return b.String()
}

func (s *Service) enforceLimits(ctx context.Context, phoneHash, deviceID, ip string) error {
	window := time.Hour
	checks := []struct {
		scope string
		id    string
		max   int64
	}{
		{"phone", phoneHash, 5},
		{"ip", ipHash(ip), 20},
		{"device", hashStr(deviceID), 10},
		{"global", "all", 1000},
	}
	for _, ck := range checks {
		n, err := s.redis.IncrWindow(ctx, ck.scope, ck.id, window)
		if err != nil {
			s.log.Error("rate limit redis failed", "err", err)
			return ErrSMSUnavailable
		}
		if n > ck.max {
			return ErrRateLimited
		}
	}
	return nil
}

func ipHash(ip string) string { return hashStr(ip) }

func hashStr(s string) string {
	sum := sha256.Sum256([]byte(s))
	return hex.EncodeToString(sum[:8])
}

func (s *Service) VerifySMS(ctx context.Context, rawPhone, code, clientDeviceID, platform, appVersion string) (*TokenPair, error) {
	e164, err := NormalizePhone(rawPhone)
	if err != nil {
		return nil, ErrInvalidPhone
	}
	if s.redis == nil {
		return nil, ErrSMSUnavailable
	}
	phoneHash := HashPhone(e164, s.cfg.PhoneHashPepper)
	stored, err := s.redis.GetCodeHash(ctx, phoneHash)
	if err != nil {
		return nil, ErrSMSUnavailable
	}
	if stored == "" {
		return nil, ErrInvalidCode
	}
	tries, err := s.redis.IncrCodeTries(ctx, phoneHash, s.cfg.SMSCodeTTL)
	if err != nil {
		return nil, ErrSMSUnavailable
	}
	if tries > 5 {
		_ = s.redis.DeleteCode(ctx, phoneHash)
		return nil, ErrInvalidCode
	}
	sum := sha256.Sum256([]byte(code + ":" + phoneHash))
	got := hex.EncodeToString(sum[:])
	if got != stored {
		return nil, ErrInvalidCode
	}
	_ = s.redis.DeleteCode(ctx, phoneHash)

	ident, err := s.store.FindIdentityByPhoneHash(phoneHash)
	if err != nil {
		return nil, err
	}
	now := s.now().UTC()
	var userID string
	if ident == nil {
		userID = id.New()
		enc, err := EncryptPhone(e164, s.cfg.PhoneEncryptionKey)
		if err != nil {
			return nil, err
		}
		user := User{ID: userID, DisplayName: "旅行者", Status: "ACTIVE", CreatedAt: now, UpdatedAt: now}
		ident := Identity{
			ID: id.New(), UserID: userID, Type: "PHONE",
			IdentifierHash: phoneHash, PhoneEncrypted: enc, VerifiedAt: now,
		}
		if err := s.store.CreateUserWithIdentity(user, ident); err != nil {
			return nil, err
		}
	} else {
		userID = ident.UserID
	}
	if s.Bootstrap != nil {
		if _, err := s.Bootstrap.EnsurePersonalSpace(userID); err != nil {
			s.log.Error("personal space bootstrap failed", "err", err)
			return nil, err
		}
	}

	if clientDeviceID == "" {
		clientDeviceID = id.New()
	}
	if platform == "" {
		platform = "unknown"
	}
	dev, err := s.store.UpsertDevice(Device{
		ID: id.New(), UserID: userID, ClientDeviceID: clientDeviceID,
		Platform: platform, AppVersion: appVersion, LastSeenAt: now,
	})
	if err != nil {
		return nil, err
	}
	return s.issuePair(userID, dev.ID, id.New(), now)
}

func (s *Service) issuePair(userID, deviceID, familyID string, now time.Time) (*TokenPair, error) {
	access, err := IssueAccessToken(s.cfg.JWTSigningKey, userID, deviceID, id.New(), s.cfg.AccessTokenTTL, now)
	if err != nil {
		return nil, err
	}
	raw, err := RandomToken()
	if err != nil {
		return nil, err
	}
	rt := RefreshToken{
		ID: id.New(), UserID: userID, DeviceID: deviceID, FamilyID: familyID,
		TokenHash: HashToken(raw), ExpiresAt: now.Add(s.cfg.RefreshTokenTTL), CreatedAt: now,
	}
	if err := s.store.InsertRefresh(rt); err != nil {
		return nil, err
	}
	display := "旅行者"
	if u, err := s.store.GetUser(userID); err == nil && u != nil && u.DisplayName != "" {
		display = u.DisplayName
	}
	return &TokenPair{
		AccessToken: access, RefreshToken: raw, TokenType: "Bearer",
		ExpiresIn: int(s.cfg.AccessTokenTTL.Seconds()), UserID: userID, DeviceID: deviceID,
		DisplayName: display,
	}, nil
}

func (s *Service) Refresh(rawToken, clientDeviceID string) (*TokenPair, error) {
	if rawToken == "" {
		return nil, ErrUnauthorized
	}
	now := s.now().UTC()
	row, err := s.store.GetRefreshByHash(HashToken(rawToken))
	if err != nil {
		return nil, err
	}
	if row == nil {
		return nil, ErrUnauthorized
	}
	if row.RevokedAt != nil || row.ReplacedBy != nil {
		_ = s.store.RevokeFamily(row.FamilyID, now)
		return nil, ErrRefreshReused
	}
	if now.After(row.ExpiresAt) {
		return nil, ErrTokenExpired
	}
	if clientDeviceID != "" {
		dev, _ := s.store.GetDevice(row.DeviceID)
		if dev != nil && dev.ClientDeviceID != "" && clientDeviceID != dev.ClientDeviceID {
			return nil, ErrUnauthorized
		}
	}
	raw, err := RandomToken()
	if err != nil {
		return nil, err
	}
	replacement := RefreshToken{
		ID: id.New(), UserID: row.UserID, DeviceID: row.DeviceID, FamilyID: row.FamilyID,
		TokenHash: HashToken(raw), ExpiresAt: now.Add(s.cfg.RefreshTokenTTL), CreatedAt: now,
	}
	if err := s.store.RotateRefresh(row.TokenHash, replacement, now); err != nil {
		return nil, err
	}
	access, err := IssueAccessToken(s.cfg.JWTSigningKey, row.UserID, row.DeviceID, id.New(), s.cfg.AccessTokenTTL, now)
	if err != nil {
		return nil, err
	}
	return &TokenPair{
		AccessToken: access, RefreshToken: raw, TokenType: "Bearer",
		ExpiresIn: int(s.cfg.AccessTokenTTL.Seconds()), UserID: row.UserID, DeviceID: row.DeviceID,
	}, nil
}

func (s *Service) Logout(rawToken, userID, deviceID string) error {
	now := s.now().UTC()
	if rawToken != "" {
		row, err := s.store.GetRefreshByHash(HashToken(rawToken))
		if err != nil {
			return err
		}
		if row != nil {
			return s.store.RevokeFamily(row.FamilyID, now)
		}
	}
	if userID != "" && deviceID != "" {
		return s.store.RevokeDeviceTokens(userID, deviceID, now)
	}
	return nil
}

func (s *Service) DeleteAccount(userID string) error {
	now := s.now().UTC()
	if err := s.store.MarkUserPendingDelete(userID, now); err != nil {
		return err
	}
	return s.store.RevokeUserTokens(userID, now)
}

func (s *Service) AccountDeleteGrace() time.Duration {
	if s.cfg.AccountDeleteGrace <= 0 {
		return 24 * time.Hour
	}
	return s.cfg.AccountDeleteGrace
}

func (s *Service) ParseAccess(token string) (*AccessClaims, error) {
	claims, err := ParseAccessToken(s.cfg.JWTSigningKey, token)
	if err != nil {
		if strings.Contains(err.Error(), "expired") {
			return nil, ErrTokenExpired
		}
		return nil, ErrUnauthorized
	}
	return claims, nil
}

func (s *Service) Me(userID string) (*Profile, error) {
	u, err := s.store.GetUser(userID)
	if err != nil {
		return nil, err
	}
	if u == nil {
		return nil, ErrUnauthorized
	}
	masked := ""
	if ident, err := s.store.FindIdentityByUserID(userID); err == nil && ident != nil {
		if ident.Type == "PASSWORD" {
			masked = u.DisplayName
		} else if phone, err := DecryptPhone(ident.PhoneEncrypted, s.cfg.PhoneEncryptionKey); err == nil {
			masked = MaskPhone(phone)
		}
	}
	spaceID := ""
	if s.Bootstrap != nil {
		spaceID, _ = s.Bootstrap.EnsurePersonalSpace(userID)
	}
	return &Profile{
		UserID: u.ID, DisplayName: u.DisplayName, Status: u.Status,
		MaskedPhone: masked, PersonalSpaceID: spaceID,
		CreatedAt: u.CreatedAt.UTC().Format(time.RFC3339Nano),
	}, nil
}

func (s *Service) PatchMe(userID, displayName string) (*Profile, error) {
	name := strings.TrimSpace(displayName)
	if name == "" || utf8.RuneCountInString(name) > 20 {
		return nil, ErrInvalidName
	}
	if err := s.store.UpdateDisplayName(userID, name, s.now().UTC()); err != nil {
		return nil, err
	}
	return s.Me(userID)
}
