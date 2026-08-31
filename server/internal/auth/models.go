package auth

import "time"

type User struct {
	ID          string
	DisplayName string
	Status      string
	CreatedAt   time.Time
	UpdatedAt   time.Time
}

type Identity struct {
	ID             string
	UserID         string
	Type           string
	IdentifierHash string
	PhoneEncrypted []byte
	VerifiedAt     time.Time
}

type Device struct {
	ID             string
	UserID         string
	ClientDeviceID string
	Platform       string
	AppVersion     string
	LastSeenAt     time.Time
}

type RefreshToken struct {
	ID         string
	UserID     string
	DeviceID   string
	FamilyID   string
	TokenHash  string
	ExpiresAt  time.Time
	RevokedAt  *time.Time
	ReplacedBy *string
	CreatedAt  time.Time
}

type Store interface {
	FindIdentityByPhoneHash(hash string) (*Identity, error)
	CreateUserWithIdentity(user User, ident Identity) error
	GetUser(id string) (*User, error)
	FindIdentityByUserID(userID string) (*Identity, error)
	UpdateDisplayName(userID, name string, at time.Time) error
	MarkUserPendingDelete(userID string, at time.Time) error
	UpdateIdentitySecret(identityID string, secret []byte, at time.Time) error

	UpsertDevice(d Device) (Device, error)
	GetDevice(id string) (*Device, error)

	InsertRefresh(t RefreshToken) error
	GetRefreshByHash(hash string) (*RefreshToken, error)
	RotateRefresh(oldHash string, replacement RefreshToken, at time.Time) error
	RevokeFamily(familyID string, at time.Time) error
	RevokeDeviceTokens(userID, deviceID string, at time.Time) error
	RevokeUserTokens(userID string, at time.Time) error

	Ping() error
}
