package auth

import (
	"fmt"
	"sync"
	"time"
)

// MemoryStore is a process-local Store used only when APP_ENV=dev and
// DEV_INMEMORY=true. Production always uses PostgreSQL.
type MemoryStore struct {
	mu       sync.Mutex
	users    map[string]User
	idents   map[string]Identity // phone hash -> identity
	devices  map[string]Device   // id
	devIndex map[string]string   // userID|clientDeviceID -> id
	refresh  map[string]RefreshToken
}

func NewMemoryStore() *MemoryStore {
	return &MemoryStore{
		users:    map[string]User{},
		idents:   map[string]Identity{},
		devices:  map[string]Device{},
		devIndex: map[string]string{},
		refresh:  map[string]RefreshToken{},
	}
}

func (s *MemoryStore) Ping() error { return nil }

func (s *MemoryStore) FindIdentityByPhoneHash(hash string) (*Identity, error) {
	s.mu.Lock()
	defer s.mu.Unlock()
	id, ok := s.idents[hash]
	if !ok {
		return nil, nil
	}
	cp := id
	return &cp, nil
}

func (s *MemoryStore) CreateUserWithIdentity(user User, ident Identity) error {
	s.mu.Lock()
	defer s.mu.Unlock()
	if _, ok := s.idents[ident.IdentifierHash]; ok {
		return fmt.Errorf("identity exists")
	}
	s.users[user.ID] = user
	s.idents[ident.IdentifierHash] = ident
	return nil
}

func (s *MemoryStore) GetUser(id string) (*User, error) {
	s.mu.Lock()
	defer s.mu.Unlock()
	u, ok := s.users[id]
	if !ok {
		return nil, nil
	}
	cp := u
	return &cp, nil
}

func (s *MemoryStore) FindIdentityByUserID(userID string) (*Identity, error) {
	s.mu.Lock()
	defer s.mu.Unlock()
	for _, it := range s.idents {
		if it.UserID == userID {
			cp := it
			return &cp, nil
		}
	}
	return nil, nil
}

func (s *MemoryStore) UpdateDisplayName(userID, name string, at time.Time) error {
	s.mu.Lock()
	defer s.mu.Unlock()
	u, ok := s.users[userID]
	if !ok {
		return fmt.Errorf("user not found")
	}
	u.DisplayName = name
	u.UpdatedAt = at
	s.users[userID] = u
	return nil
}

func (s *MemoryStore) MarkUserPendingDelete(userID string, at time.Time) error {
	s.mu.Lock()
	defer s.mu.Unlock()
	u, ok := s.users[userID]
	if !ok {
		return fmt.Errorf("user not found")
	}
	u.Status = "PENDING_DELETE"
	u.UpdatedAt = at
	s.users[userID] = u
	return nil
}

func (s *MemoryStore) UpdateIdentitySecret(identityID string, secret []byte, at time.Time) error {
	s.mu.Lock()
	defer s.mu.Unlock()
	for hash, it := range s.idents {
		if it.ID == identityID {
			it.PhoneEncrypted = secret
			it.VerifiedAt = at
			s.idents[hash] = it
			return nil
		}
	}
	return fmt.Errorf("identity not found")
}

func (s *MemoryStore) UpsertDevice(d Device) (Device, error) {
	s.mu.Lock()
	defer s.mu.Unlock()
	key := d.UserID + "|" + d.ClientDeviceID
	if id, ok := s.devIndex[key]; ok {
		old := s.devices[id]
		old.Platform = d.Platform
		old.AppVersion = d.AppVersion
		old.LastSeenAt = d.LastSeenAt
		s.devices[id] = old
		return old, nil
	}
	s.devices[d.ID] = d
	s.devIndex[key] = d.ID
	return d, nil
}

func (s *MemoryStore) GetDevice(id string) (*Device, error) {
	s.mu.Lock()
	defer s.mu.Unlock()
	d, ok := s.devices[id]
	if !ok {
		return nil, nil
	}
	cp := d
	return &cp, nil
}

func (s *MemoryStore) InsertRefresh(t RefreshToken) error {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.refresh[t.TokenHash] = t
	return nil
}

func (s *MemoryStore) GetRefreshByHash(hash string) (*RefreshToken, error) {
	s.mu.Lock()
	defer s.mu.Unlock()
	t, ok := s.refresh[hash]
	if !ok {
		return nil, nil
	}
	cp := t
	return &cp, nil
}

func (s *MemoryStore) RotateRefresh(oldHash string, replacement RefreshToken, at time.Time) error {
	s.mu.Lock()
	defer s.mu.Unlock()
	old, ok := s.refresh[oldHash]
	if !ok {
		return fmt.Errorf("refresh not found")
	}
	old.RevokedAt = &at
	id := replacement.ID
	old.ReplacedBy = &id
	s.refresh[oldHash] = old
	s.refresh[replacement.TokenHash] = replacement
	return nil
}

func (s *MemoryStore) RevokeFamily(familyID string, at time.Time) error {
	s.mu.Lock()
	defer s.mu.Unlock()
	for k, t := range s.refresh {
		if t.FamilyID == familyID && t.RevokedAt == nil {
			t.RevokedAt = &at
			s.refresh[k] = t
		}
	}
	return nil
}

func (s *MemoryStore) RevokeDeviceTokens(userID, deviceID string, at time.Time) error {
	s.mu.Lock()
	defer s.mu.Unlock()
	for k, t := range s.refresh {
		if t.UserID == userID && t.DeviceID == deviceID && t.RevokedAt == nil {
			t.RevokedAt = &at
			s.refresh[k] = t
		}
	}
	return nil
}

func (s *MemoryStore) RevokeUserTokens(userID string, at time.Time) error {
	s.mu.Lock()
	defer s.mu.Unlock()
	for k, t := range s.refresh {
		if t.UserID == userID && t.RevokedAt == nil {
			t.RevokedAt = &at
			s.refresh[k] = t
		}
	}
	return nil
}
