package ledger

import (
	"fmt"
	"sync"
	"time"

	"github.com/skydream9527-ctrl/xiaoquexing-server/internal/growth"
	"github.com/skydream9527-ctrl/xiaoquexing-server/internal/id"
)

type Memory struct {
	mu        sync.Mutex
	spaces    map[string]Space
	byOwner   map[string]string // userID -> personal space
	members   map[string]Member
	records   map[string]Record
	stats     map[string]DailyStats
	mutations map[string]AppliedMutation
	changes   []Change
	seq       int64
}

func NewMemory() *Memory {
	return &Memory{
		spaces:    map[string]Space{},
		byOwner:   map[string]string{},
		members:   map[string]Member{},
		records:   map[string]Record{},
		stats:     map[string]DailyStats{},
		mutations: map[string]AppliedMutation{},
	}
}

func memberKey(spaceID, userID string) string { return spaceID + "|" + userID }
func statKey(spaceID, date string) string     { return spaceID + "|" + date }

func (m *Memory) EnsurePersonalSpace(userID, name string) (Space, error) {
	m.mu.Lock()
	defer m.mu.Unlock()
	if sid, ok := m.byOwner[userID]; ok {
		return m.spaces[sid], nil
	}
	now := time.Now().UTC()
	sp := Space{
		ID: id.New(), Name: name, SpaceType: "PERSONAL", OwnerID: userID,
		ActivePlantType: "TREE", PlantStage: growth.StageFromGP(0), Timezone: "Asia/Shanghai",
		Version: 1, CreatedAt: now,
	}
	m.spaces[sp.ID] = sp
	m.byOwner[userID] = sp.ID
	m.members[memberKey(sp.ID, userID)] = Member{SpaceID: sp.ID, UserID: userID, Role: "OWNER", Status: "ACTIVE"}
	return sp, nil
}

func (m *Memory) ListSpacesForUser(userID string) ([]Space, error) {
	m.mu.Lock()
	defer m.mu.Unlock()
	out := []Space{}
	for _, mem := range m.members {
		if mem.UserID == userID && mem.Status == "ACTIVE" {
			if sp, ok := m.spaces[mem.SpaceID]; ok {
				out = append(out, sp)
			}
		}
	}
	return out, nil
}

func (m *Memory) GetSpace(id string) (*Space, error) {
	m.mu.Lock()
	defer m.mu.Unlock()
	sp, ok := m.spaces[id]
	if !ok {
		return nil, nil
	}
	cp := sp
	return &cp, nil
}

func (m *Memory) GetMember(spaceID, userID string) (*Member, error) {
	m.mu.Lock()
	defer m.mu.Unlock()
	mem, ok := m.members[memberKey(spaceID, userID)]
	if !ok {
		return nil, nil
	}
	cp := mem
	return &cp, nil
}

func (m *Memory) GetRecord(id string) (*Record, error) {
	m.mu.Lock()
	defer m.mu.Unlock()
	r, ok := m.records[id]
	if !ok {
		return nil, nil
	}
	cp := r
	return &cp, nil
}

func (m *Memory) LiveRecords(spaceID string) ([]Record, error) {
	m.mu.Lock()
	defer m.mu.Unlock()
	out := []Record{}
	for _, r := range m.records {
		if r.SpaceID == spaceID && r.DeletedAt == nil {
			out = append(out, r)
		}
	}
	return out, nil
}

func (m *Memory) ListRecords(spaceID, afterOccurred, afterID string, limit int) ([]Record, error) {
	m.mu.Lock()
	defer m.mu.Unlock()
	out := []Record{}
	for _, r := range m.records {
		if r.SpaceID != spaceID || r.DeletedAt != nil {
			continue
		}
		key := r.OccurredAt.UTC().Format(time.RFC3339Nano) + "|" + r.ID
		if afterOccurred != "" {
			cur := afterOccurred + "|" + afterID
			if key >= cur {
				continue
			}
		}
		out = append(out, r)
	}
	sortRecords(out)
	if limit > 0 && len(out) > limit {
		out = out[:limit]
	}
	return out, nil
}

func (m *Memory) ListChanges(userID string, afterSeq int64, limit int) ([]Change, error) {
	m.mu.Lock()
	defer m.mu.Unlock()
	allowed := map[string]struct{}{}
	for _, mem := range m.members {
		if mem.UserID == userID && mem.Status == "ACTIVE" {
			allowed[mem.SpaceID] = struct{}{}
		}
	}
	out := []Change{}
	for _, c := range m.changes {
		if c.Sequence <= afterSeq {
			continue
		}
		if _, ok := allowed[c.SpaceID]; !ok {
			continue
		}
		out = append(out, c)
		if limit > 0 && len(out) >= limit {
			break
		}
	}
	return out, nil
}

func (m *Memory) ApplyTx(fn func(tx Tx) error) error {
	m.mu.Lock()
	defer m.mu.Unlock()
	return fn(&memTx{m: m})
}

type memTx struct{ m *Memory }

func (t *memTx) GetMember(spaceID, userID string) (*Member, error) {
	mem, ok := t.m.members[memberKey(spaceID, userID)]
	if !ok {
		return nil, nil
	}
	cp := mem
	return &cp, nil
}
func (t *memTx) GetSpace(id string) (*Space, error) {
	sp, ok := t.m.spaces[id]
	if !ok {
		return nil, nil
	}
	cp := sp
	return &cp, nil
}
func (t *memTx) GetRecord(id string) (*Record, error) {
	r, ok := t.m.records[id]
	if !ok {
		return nil, nil
	}
	cp := r
	return &cp, nil
}
func (t *memTx) LiveRecords(spaceID string) ([]Record, error) {
	out := []Record{}
	for _, r := range t.m.records {
		if r.SpaceID == spaceID && r.DeletedAt == nil {
			out = append(out, r)
		}
	}
	return out, nil
}
func (t *memTx) UpsertRecord(r Record) error {
	t.m.records[r.ID] = r
	return nil
}
func (t *memTx) LockDailyStats(spaceID, date string) (DailyStats, error) {
	if s, ok := t.m.stats[statKey(spaceID, date)]; ok {
		return s, nil
	}
	return DailyStats{SpaceID: spaceID, OccurredDate: date}, nil
}
func (t *memTx) SaveDailyStats(s DailyStats) error {
	t.m.stats[statKey(s.SpaceID, s.OccurredDate)] = s
	return nil
}
func (t *memTx) SetSpaceTotals(spaceID string, totalGP int64, stage string) error {
	sp, ok := t.m.spaces[spaceID]
	if !ok {
		return fmt.Errorf("space not found")
	}
	sp.TotalGP = totalGP
	sp.PlantStage = stage
	sp.Version++
	t.m.spaces[spaceID] = sp
	return nil
}
func (t *memTx) NextSeq() (int64, error) {
	t.m.seq++
	return t.m.seq, nil
}
func (t *memTx) InsertChange(c Change) error {
	t.m.changes = append(t.m.changes, c)
	return nil
}
func (t *memTx) GetMutation(id string) (*AppliedMutation, error) {
	m, ok := t.m.mutations[id]
	if !ok {
		return nil, nil
	}
	cp := m
	return &cp, nil
}
func (t *memTx) SaveMutation(m AppliedMutation) error {
	t.m.mutations[m.MutationID] = m
	return nil
}

func sortRecords(out []Record) {
	for i := 0; i < len(out); i++ {
		for j := i + 1; j < len(out); j++ {
			if out[j].OccurredAt.After(out[i].OccurredAt) ||
				(out[j].OccurredAt.Equal(out[i].OccurredAt) && out[j].ID > out[i].ID) {
				out[i], out[j] = out[j], out[i]
			}
		}
	}
}
