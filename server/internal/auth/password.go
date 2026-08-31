package auth

import (
	"errors"
	"strings"
	"unicode"

	"github.com/skydream9527-ctrl/xiaoquexing-server/internal/id"
	"golang.org/x/crypto/bcrypt"
)

var (
	ErrInvalidAccount     = errors.New("invalid account")
	ErrWeakPassword       = errors.New("weak password")
	ErrAccountTaken       = errors.New("account taken")
	ErrInvalidCredentials = errors.New("invalid credentials")
)

func NormalizeAccount(raw string) (string, error) {
	name := strings.ToLower(strings.TrimSpace(raw))
	if len(name) < 3 || len(name) > 32 {
		return "", ErrInvalidAccount
	}
	for _, r := range name {
		if !(unicode.IsLetter(r) || unicode.IsDigit(r) || r == '_') {
			return "", ErrInvalidAccount
		}
	}
	return name, nil
}

func HashAccount(name, pepper string) string {
	return HashPhone(name, pepper)
}

func hashPassword(raw string) ([]byte, error) {
	if len(raw) < 6 || len(raw) > 72 {
		return nil, ErrWeakPassword
	}
	return bcrypt.GenerateFromPassword([]byte(raw), bcrypt.DefaultCost)
}

func (s *Service) ChangePassword(userID, oldPassword, newPassword string) error {
	ident, err := s.passwordIdent(userID)
	if err != nil {
		return err
	}
	if bcrypt.CompareHashAndPassword(ident.PhoneEncrypted, []byte(oldPassword)) != nil {
		return ErrInvalidCredentials
	}
	return s.writePassword(ident.ID, newPassword)
}

func (s *Service) ResetPasswordOnDevice(userID, newPassword string) error {
	ident, err := s.passwordIdent(userID)
	if err != nil {
		return err
	}
	return s.writePassword(ident.ID, newPassword)
}

func (s *Service) passwordIdent(userID string) (*Identity, error) {
	ident, err := s.store.FindIdentityByUserID(userID)
	if err != nil {
		return nil, err
	}
	if ident == nil || ident.Type != "PASSWORD" {
		return nil, ErrInvalidCredentials
	}
	return ident, nil
}

func (s *Service) writePassword(identityID, newPassword string) error {
	secret, err := hashPassword(newPassword)
	if err != nil {
		return err
	}
	return s.store.UpdateIdentitySecret(identityID, secret, s.now().UTC())
}

func (s *Service) Register(account, password, clientDeviceID, platform, appVersion string) (*TokenPair, error) {
	name, err := NormalizeAccount(account)
	if err != nil {
		return nil, err
	}
	secret, err := hashPassword(password)
	if err != nil {
		return nil, err
	}
	hash := HashAccount(name, s.cfg.PhoneHashPepper)
	existing, err := s.store.FindIdentityByPhoneHash(hash)
	if err != nil {
		return nil, err
	}
	if existing != nil {
		return nil, ErrAccountTaken
	}
	now := s.now().UTC()
	userID := id.New()
	user := User{ID: userID, DisplayName: name, Status: "ACTIVE", CreatedAt: now, UpdatedAt: now}
	ident := Identity{
		ID: id.New(), UserID: userID, Type: "PASSWORD",
		IdentifierHash: hash, PhoneEncrypted: secret, VerifiedAt: now,
	}
	if err := s.store.CreateUserWithIdentity(user, ident); err != nil {
		return nil, err
	}
	return s.completeLogin(userID, clientDeviceID, platform, appVersion)
}

func (s *Service) Login(account, password, clientDeviceID, platform, appVersion string) (*TokenPair, error) {
	name, err := NormalizeAccount(account)
	if err != nil {
		return nil, ErrInvalidCredentials
	}
	hash := HashAccount(name, s.cfg.PhoneHashPepper)
	ident, err := s.store.FindIdentityByPhoneHash(hash)
	if err != nil {
		return nil, err
	}
	if ident == nil || ident.Type != "PASSWORD" {
		return nil, ErrInvalidCredentials
	}
	if bcrypt.CompareHashAndPassword(ident.PhoneEncrypted, []byte(password)) != nil {
		return nil, ErrInvalidCredentials
	}
	return s.completeLogin(ident.UserID, clientDeviceID, platform, appVersion)
}

func (s *Service) completeLogin(userID, clientDeviceID, platform, appVersion string) (*TokenPair, error) {
	now := s.now().UTC()
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
