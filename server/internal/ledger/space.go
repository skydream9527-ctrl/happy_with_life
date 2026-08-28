package ledger

import (
	"crypto/rand"
	"crypto/sha256"
	"encoding/hex"
	"errors"
	"strings"
	"time"
	"unicode/utf8"

	"github.com/skydream9527-ctrl/xiaoquexing-server/internal/growth"
	"github.com/skydream9527-ctrl/xiaoquexing-server/internal/id"
)

var (
	ErrInviteExpired = errors.New("invite expired")
	ErrInviteRevoked = errors.New("invite revoked")
	ErrSpaceFull     = errors.New("space full")
	ErrOwnerLeave    = errors.New("owner cannot leave")
	ErrPersonalSpace = errors.New("personal space")
)

func HashInviteToken(raw string) string {
	sum := sha256.Sum256([]byte(raw))
	return hex.EncodeToString(sum[:])
}

func newInviteToken() (string, error) {
	b := make([]byte, 16)
	if _, err := rand.Read(b); err != nil {
		return "", err
	}
	return hex.EncodeToString(b), nil
}

func (s *Service) CreateShared(userID, name, spaceType, plantType, tz string) (Space, error) {
	name = strings.TrimSpace(name)
	if name == "" || utf8.RuneCountInString(name) > 40 {
		return Space{}, ErrInvalid
	}
	st := strings.ToUpper(strings.TrimSpace(spaceType))
	if st != "COUPLE" && st != "FAMILY" && st != "FRIEND" {
		return Space{}, ErrInvalid
	}
	plant := strings.ToUpper(strings.TrimSpace(plantType))
	if plant == "" {
		plant = "TREE"
	}
	if len(plant) > 30 {
		return Space{}, ErrInvalid
	}
	if tz == "" {
		tz = "Asia/Shanghai"
	}
	if _, err := time.LoadLocation(tz); err != nil {
		return Space{}, ErrInvalid
	}
	now := s.Now().UTC()
	sp := Space{
		ID: id.New(), Name: name, SpaceType: st, OwnerID: userID,
		ActivePlantType: plant, PlantStage: growth.StageFromGP(0), Timezone: tz,
		Version: 1, CreatedAt: now,
	}
	owner := Member{SpaceID: sp.ID, UserID: userID, Role: "OWNER", Status: "ACTIVE"}
	if err := s.Store.CreateSpace(sp, owner); err != nil {
		return Space{}, err
	}
	return sp, nil
}

func (s *Service) PatchSpace(userID, spaceID, name, plantType string) (Space, error) {
	sp, mem, err := s.requireActive(userID, spaceID)
	if err != nil {
		return Space{}, err
	}
	if mem.Role != "OWNER" && mem.Role != "ADMIN" {
		return Space{}, ErrForbidden
	}
	if name = strings.TrimSpace(name); name != "" {
		if utf8.RuneCountInString(name) > 40 {
			return Space{}, ErrInvalid
		}
		sp.Name = name
	}
	if plantType = strings.ToUpper(strings.TrimSpace(plantType)); plantType != "" {
		if len(plantType) > 30 {
			return Space{}, ErrInvalid
		}
		sp.ActivePlantType = plantType
	}
	if err := s.Store.UpdateSpace(*sp); err != nil {
		return Space{}, err
	}
	return *sp, nil
}

func (s *Service) Members(userID, spaceID string) ([]Member, error) {
	if _, _, err := s.requireActive(userID, spaceID); err != nil {
		return nil, err
	}
	return s.Store.ListMembers(spaceID)
}

func (s *Service) CreateInvite(userID, spaceID, publicBase string) (raw string, link string, inv Invite, err error) {
	sp, mem, err := s.requireActive(userID, spaceID)
	if err != nil {
		return "", "", Invite{}, err
	}
	if sp.SpaceType == "PERSONAL" {
		return "", "", Invite{}, ErrPersonalSpace
	}
	if mem.Role != "OWNER" && mem.Role != "ADMIN" {
		return "", "", Invite{}, ErrForbidden
	}
	raw, err = newInviteToken()
	if err != nil {
		return "", "", Invite{}, err
	}
	now := s.Now().UTC()
	inv = Invite{
		ID: id.New(), SpaceID: spaceID, InviterID: userID,
		TokenHash: HashInviteToken(raw), ExpiresAt: now.Add(InviteTTL),
		MaxUses: InviteMaxUses, CreatedAt: now,
	}
	if err := s.Store.InsertInvite(inv); err != nil {
		return "", "", Invite{}, err
	}
	base := strings.TrimRight(publicBase, "/")
	if base == "" {
		link = "/join/" + raw
	} else {
		link = base + "/join/" + raw
	}
	return raw, link, inv, nil
}

func (s *Service) PeekInvite(raw string) (*Space, *Invite, error) {
	inv, err := s.Store.GetInviteByHash(HashInviteToken(strings.TrimSpace(raw)))
	if err != nil {
		return nil, nil, err
	}
	if err := inviteOK(inv, s.Now().UTC()); err != nil {
		return nil, inv, err
	}
	sp, err := s.Store.GetSpace(inv.SpaceID)
	if err != nil || sp == nil {
		return nil, inv, ErrNotFound
	}
	return sp, inv, nil
}

func (s *Service) AcceptInvite(userID, raw string) (Space, error) {
	inv, err := s.Store.GetInviteByHash(HashInviteToken(strings.TrimSpace(raw)))
	if err != nil {
		return Space{}, err
	}
	now := s.Now().UTC()
	if err := inviteOK(inv, now); err != nil {
		return Space{}, err
	}
	sp, err := s.Store.GetSpace(inv.SpaceID)
	if err != nil || sp == nil {
		return Space{}, ErrNotFound
	}
	if sp.SpaceType == "PERSONAL" {
		return Space{}, ErrPersonalSpace
	}
	existing, err := s.Store.GetMember(sp.ID, userID)
	if err != nil {
		return Space{}, err
	}
	if existing != nil && existing.Status == "ACTIVE" {
		return *sp, nil
	}
	n, err := s.Store.CountActiveMembers(sp.ID)
	if err != nil {
		return Space{}, err
	}
	if n >= MaxSpaceMembers {
		return Space{}, ErrSpaceFull
	}
	mem := Member{SpaceID: sp.ID, UserID: userID, Role: "MEMBER", Status: "ACTIVE"}
	if existing != nil {
		mem.ContributedGP = existing.ContributedGP
	}
	if err := s.Store.SaveMember(mem); err != nil {
		return Space{}, err
	}
	inv.UsedCount++
	if err := s.Store.UpdateInvite(*inv); err != nil {
		return Space{}, err
	}
	return *sp, nil
}

func (s *Service) RevokeInvite(userID, spaceID, inviteID string) error {
	_, mem, err := s.requireActive(userID, spaceID)
	if err != nil {
		return err
	}
	if mem.Role != "OWNER" && mem.Role != "ADMIN" {
		return ErrForbidden
	}
	inv, err := s.Store.GetInvite(inviteID)
	if err != nil || inv == nil || inv.SpaceID != spaceID {
		return ErrNotFound
	}
	now := s.Now().UTC()
	inv.RevokedAt = &now
	return s.Store.UpdateInvite(*inv)
}

func (s *Service) ListInvites(userID, spaceID string) ([]Invite, error) {
	_, mem, err := s.requireActive(userID, spaceID)
	if err != nil {
		return nil, err
	}
	if mem.Role != "OWNER" && mem.Role != "ADMIN" {
		return nil, ErrForbidden
	}
	return s.Store.ListInvites(spaceID)
}

func (s *Service) Leave(userID, spaceID string) error {
	sp, mem, err := s.requireActive(userID, spaceID)
	if err != nil {
		return err
	}
	if sp.SpaceType == "PERSONAL" {
		return ErrPersonalSpace
	}
	if mem.Role == "OWNER" {
		return ErrOwnerLeave
	}
	now := s.Now().UTC()
	_ = now
	mem.Status = "LEFT"
	return s.Store.SaveMember(*mem)
}

func (s *Service) Kick(actorID, spaceID, targetID string) error {
	_, mem, err := s.requireActive(actorID, spaceID)
	if err != nil {
		return err
	}
	if mem.Role != "OWNER" && mem.Role != "ADMIN" {
		return ErrForbidden
	}
	if targetID == actorID {
		return ErrInvalid
	}
	target, err := s.Store.GetMember(spaceID, targetID)
	if err != nil || target == nil {
		return ErrNotFound
	}
	if target.Role == "OWNER" {
		return ErrForbidden
	}
	target.Status = "REMOVED"
	return s.Store.SaveMember(*target)
}

func (s *Service) requireActive(userID, spaceID string) (*Space, *Member, error) {
	sp, err := s.Store.GetSpace(spaceID)
	if err != nil {
		return nil, nil, err
	}
	if sp == nil {
		return nil, nil, ErrNotFound
	}
	mem, err := s.Store.GetMember(spaceID, userID)
	if err != nil {
		return nil, nil, err
	}
	if mem == nil || mem.Status != "ACTIVE" {
		return nil, nil, ErrForbidden
	}
	return sp, mem, nil
}

func inviteOK(inv *Invite, now time.Time) error {
	if inv == nil {
		return ErrNotFound
	}
	if inv.RevokedAt != nil {
		return ErrInviteRevoked
	}
	if !inv.ExpiresAt.After(now) {
		return ErrInviteExpired
	}
	if inv.UsedCount >= inv.MaxUses {
		return ErrInviteExpired
	}
	return nil
}
