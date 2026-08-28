package auth

import (
	"context"
	"errors"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

type PGStore struct {
	pool *pgxpool.Pool
}

func NewPGStore(pool *pgxpool.Pool) *PGStore { return &PGStore{pool: pool} }

func (s *PGStore) Ping() error {
	ctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
	defer cancel()
	return s.pool.Ping(ctx)
}

func (s *PGStore) FindIdentityByPhoneHash(hash string) (*Identity, error) {
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	row := s.pool.QueryRow(ctx, `
		select id, user_id, type, identifier_hash, phone_encrypted, verified_at
		from auth_identities where identifier_hash = $1`, hash)
	var it Identity
	err := row.Scan(&it.ID, &it.UserID, &it.Type, &it.IdentifierHash, &it.PhoneEncrypted, &it.VerifiedAt)
	if errors.Is(err, pgx.ErrNoRows) {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	return &it, nil
}

func (s *PGStore) CreateUserWithIdentity(user User, ident Identity) error {
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	tx, err := s.pool.Begin(ctx)
	if err != nil {
		return err
	}
	defer tx.Rollback(ctx)
	_, err = tx.Exec(ctx, `
		insert into users (id, display_name, status, created_at, updated_at)
		values ($1,$2,$3,$4,$5)`, user.ID, user.DisplayName, user.Status, user.CreatedAt, user.UpdatedAt)
	if err != nil {
		return err
	}
	_, err = tx.Exec(ctx, `
		insert into auth_identities (id, user_id, type, identifier_hash, phone_encrypted, verified_at, created_at, updated_at)
		values ($1,$2,$3,$4,$5,$6,$7,$8)`,
		ident.ID, ident.UserID, ident.Type, ident.IdentifierHash, ident.PhoneEncrypted, ident.VerifiedAt, user.CreatedAt, user.UpdatedAt)
	if err != nil {
		return err
	}
	return tx.Commit(ctx)
}

func (s *PGStore) FindIdentityByUserID(userID string) (*Identity, error) {
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	row := s.pool.QueryRow(ctx, `
		select id, user_id, type, identifier_hash, phone_encrypted, verified_at
		from auth_identities where user_id=$1 and type='PHONE' limit 1`, userID)
	var it Identity
	err := row.Scan(&it.ID, &it.UserID, &it.Type, &it.IdentifierHash, &it.PhoneEncrypted, &it.VerifiedAt)
	if errors.Is(err, pgx.ErrNoRows) {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	return &it, nil
}

func (s *PGStore) UpdateDisplayName(userID, name string, at time.Time) error {
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	tag, err := s.pool.Exec(ctx, `update users set display_name=$2, updated_at=$3 where id=$1`, userID, name, at)
	if err != nil {
		return err
	}
	if tag.RowsAffected() == 0 {
		return errors.New("user not found")
	}
	return nil
}

func (s *PGStore) GetUser(id string) (*User, error) {
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	row := s.pool.QueryRow(ctx, `select id, display_name, status, created_at, updated_at from users where id=$1`, id)
	var u User
	err := row.Scan(&u.ID, &u.DisplayName, &u.Status, &u.CreatedAt, &u.UpdatedAt)
	if errors.Is(err, pgx.ErrNoRows) {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	return &u, nil
}

func (s *PGStore) MarkUserPendingDelete(userID string, at time.Time) error {
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	_, err := s.pool.Exec(ctx, `update users set status='PENDING_DELETE', delete_requested_at=$2, updated_at=$2 where id=$1`, userID, at)
	return err
}

func (s *PGStore) UpsertDevice(d Device) (Device, error) {
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	row := s.pool.QueryRow(ctx, `
		insert into devices (id, user_id, client_device_id, platform, app_version, last_seen_at, created_at, updated_at)
		values ($1,$2,$3,$4,$5,$6,$6,$6)
		on conflict (user_id, client_device_id)
		do update set platform=excluded.platform, app_version=excluded.app_version, last_seen_at=excluded.last_seen_at, updated_at=excluded.updated_at
		returning id, user_id, client_device_id, platform, app_version, last_seen_at`,
		d.ID, d.UserID, d.ClientDeviceID, d.Platform, d.AppVersion, d.LastSeenAt)
	var out Device
	err := row.Scan(&out.ID, &out.UserID, &out.ClientDeviceID, &out.Platform, &out.AppVersion, &out.LastSeenAt)
	return out, err
}

func (s *PGStore) GetDevice(id string) (*Device, error) {
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	row := s.pool.QueryRow(ctx, `select id, user_id, client_device_id, platform, app_version, last_seen_at from devices where id=$1`, id)
	var d Device
	err := row.Scan(&d.ID, &d.UserID, &d.ClientDeviceID, &d.Platform, &d.AppVersion, &d.LastSeenAt)
	if errors.Is(err, pgx.ErrNoRows) {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	return &d, nil
}

func (s *PGStore) InsertRefresh(t RefreshToken) error {
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	_, err := s.pool.Exec(ctx, `
		insert into refresh_tokens (id, user_id, device_id, family_id, token_hash, expires_at, created_at, updated_at)
		values ($1,$2,$3,$4,$5,$6,$7,$7)`,
		t.ID, t.UserID, t.DeviceID, t.FamilyID, t.TokenHash, t.ExpiresAt, t.CreatedAt)
	return err
}

func (s *PGStore) GetRefreshByHash(hash string) (*RefreshToken, error) {
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	row := s.pool.QueryRow(ctx, `
		select id, user_id, device_id, family_id, token_hash, expires_at, revoked_at, replaced_by, created_at
		from refresh_tokens where token_hash=$1`, hash)
	var t RefreshToken
	err := row.Scan(&t.ID, &t.UserID, &t.DeviceID, &t.FamilyID, &t.TokenHash, &t.ExpiresAt, &t.RevokedAt, &t.ReplacedBy, &t.CreatedAt)
	if errors.Is(err, pgx.ErrNoRows) {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	return &t, nil
}

func (s *PGStore) RotateRefresh(oldHash string, replacement RefreshToken, at time.Time) error {
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	tx, err := s.pool.Begin(ctx)
	if err != nil {
		return err
	}
	defer tx.Rollback(ctx)
	_, err = tx.Exec(ctx, `update refresh_tokens set revoked_at=$2, replaced_by=$3, updated_at=$2 where token_hash=$1`,
		oldHash, at, replacement.ID)
	if err != nil {
		return err
	}
	_, err = tx.Exec(ctx, `
		insert into refresh_tokens (id, user_id, device_id, family_id, token_hash, expires_at, created_at, updated_at)
		values ($1,$2,$3,$4,$5,$6,$7,$7)`,
		replacement.ID, replacement.UserID, replacement.DeviceID, replacement.FamilyID, replacement.TokenHash, replacement.ExpiresAt, replacement.CreatedAt)
	if err != nil {
		return err
	}
	return tx.Commit(ctx)
}

func (s *PGStore) RevokeFamily(familyID string, at time.Time) error {
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	_, err := s.pool.Exec(ctx, `update refresh_tokens set revoked_at=$2, updated_at=$2 where family_id=$1 and revoked_at is null`, familyID, at)
	return err
}

func (s *PGStore) RevokeDeviceTokens(userID, deviceID string, at time.Time) error {
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	_, err := s.pool.Exec(ctx, `update refresh_tokens set revoked_at=$3, updated_at=$3 where user_id=$1 and device_id=$2 and revoked_at is null`, userID, deviceID, at)
	return err
}

func (s *PGStore) RevokeUserTokens(userID string, at time.Time) error {
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	_, err := s.pool.Exec(ctx, `update refresh_tokens set revoked_at=$2, updated_at=$2 where user_id=$1 and revoked_at is null`, userID, at)
	return err
}
