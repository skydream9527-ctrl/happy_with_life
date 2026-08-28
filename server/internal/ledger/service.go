package ledger

import (
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"strings"
	"time"
	"unicode/utf8"

	"github.com/skydream9527-ctrl/xiaoquexing-server/internal/growth"
	"github.com/skydream9527-ctrl/xiaoquexing-server/internal/id"
)

var (
	ErrForbidden     = errors.New("space forbidden")
	ErrInvalid       = errors.New("record invalid")
	ErrConflict      = errors.New("record version conflict")
	ErrDeleted       = errors.New("record deleted")
	ErrMutationReuse = errors.New("mutation id reused")
	ErrDependency    = errors.New("mutation dependency missing")
	ErrNotFound      = errors.New("not found")
)

type Service struct {
	Store Store
	Media MediaHook
	Now   func() time.Time
}

// MediaHook is implemented by internal/media.Service. Nil is allowed in tests
// that never attach photos.
type MediaHook interface {
	Resolve(userID, recordID string, types []struct {
		MediaID string
		Type    string
	}) (photoCount int, hasVoice bool, ids []string, err error)
	Bind(recordID string, ids []string) error
}

func NewService(st Store) *Service {
	return &Service{Store: st, Now: time.Now}
}

func (s *Service) EnsurePersonalSpace(userID string) (string, error) {
	sp, err := s.Store.EnsurePersonalSpace(userID, "我的小确幸")
	if err != nil {
		return "", err
	}
	return sp.ID, nil
}

func HashMutation(m Mutation) string {
	b, _ := json.Marshal(m)
	sum := sha256.Sum256(b)
	return hex.EncodeToString(sum[:])
}

func (s *Service) Push(userID, deviceID string, batch []Mutation) []MutationResult {
	out := make([]MutationResult, 0, len(batch))
	if len(batch) > 100 {
		batch = batch[:100]
	}
	applied := map[string]bool{}
	for _, m := range batch {
		if m.DependsOnMutationID != "" && !applied[m.DependsOnMutationID] {
			prev, _ := s.lookupMutation(m.DependsOnMutationID)
			if prev == nil {
				out = append(out, reject(m, "MUTATION_DEPENDENCY_MISSING", "缺少前置 mutation", false))
				continue
			}
		}
		res := s.Apply(userID, deviceID, m)
		if res.Status == "APPLIED" || res.Status == "DUPLICATE" {
			applied[m.MutationID] = true
		}
		out = append(out, res)
	}
	return out
}

func (s *Service) lookupMutation(id string) (*AppliedMutation, error) {
	var found *AppliedMutation
	err := s.Store.ApplyTx(func(tx Tx) error {
		m, err := tx.GetMutation(id)
		found = m
		return err
	})
	return found, err
}

func (s *Service) Apply(userID, deviceID string, m Mutation) MutationResult {
	if m.MutationID == "" {
		m.MutationID = id.New()
	}
	hash := HashMutation(m)
	var result MutationResult
	err := s.Store.ApplyTx(func(tx Tx) error {
		if existing, err := tx.GetMutation(m.MutationID); err != nil {
			return err
		} else if existing != nil {
			if existing.RequestHash == hash {
				_ = json.Unmarshal(existing.ResponseJSON, &result)
				result.Status = "DUPLICATE"
				result.MutationID = m.MutationID
				return nil
			}
			result = reject(m, "MUTATION_ID_REUSED", "mutationId 已被不同请求使用", false)
			return nil
		}
		if m.DependsOnMutationID != "" {
			dep, err := tx.GetMutation(m.DependsOnMutationID)
			if err != nil {
				return err
			}
			if dep == nil {
				result = reject(m, "MUTATION_DEPENDENCY_MISSING", "缺少前置 mutation", false)
				return nil
			}
		}
		res, err := s.applyLocked(tx, userID, m)
		if err != nil {
			return err
		}
		result = res
		raw, _ := json.Marshal(res)
		return tx.SaveMutation(AppliedMutation{
			MutationID: m.MutationID, UserID: userID, DeviceID: deviceID,
			RequestHash: hash, ResponseJSON: raw,
		})
	})
	if err != nil {
		return reject(m, "INTERNAL_ERROR", "写入失败", true)
	}
	return result
}

func (s *Service) applyLocked(tx Tx, userID string, m Mutation) (MutationResult, error) {
	now := s.Now().UTC()
	switch strings.ToUpper(m.EntityType) {
	case "RECORD", "":
	default:
		return reject(m, "RECORD_INVALID", "暂不支持该实体类型", false), nil
	}
	op := strings.ToUpper(m.Operation)
	if op == "" {
		op = "UPSERT"
	}

	spaceID := m.Payload.SpaceID
	if m.ServerID != "" {
		cur, err := tx.GetRecord(m.ServerID)
		if err != nil {
			return MutationResult{}, err
		}
		if cur != nil {
			spaceID = cur.SpaceID
		}
	}
	if spaceID == "" {
		return reject(m, "RECORD_INVALID", "缺少 spaceId", false), nil
	}
	mem, err := tx.GetMember(spaceID, userID)
	if err != nil {
		return MutationResult{}, err
	}
	if mem == nil || mem.Status != "ACTIVE" {
		return reject(m, "SPACE_FORBIDDEN", "无权访问该空间", false), nil
	}

	if op == "DELETE" {
		return s.applyDelete(tx, userID, m, now)
	}
	return s.applyUpsert(tx, userID, m, now)
}

func (s *Service) applyDelete(tx Tx, userID string, m Mutation, now time.Time) (MutationResult, error) {
	if m.ServerID == "" {
		return reject(m, "RECORD_INVALID", "删除必须带 serverId", false), nil
	}
	cur, err := tx.GetRecord(m.ServerID)
	if err != nil {
		return MutationResult{}, err
	}
	if cur == nil {
		return reject(m, "RECORD_INVALID", "记录不存在", false), nil
	}
	if cur.AuthorID != userID {
		return reject(m, "SPACE_FORBIDDEN", "只有作者可以删除", false), nil
	}
	if cur.DeletedAt != nil {
		return reject(m, "RECORD_DELETED", "记录已删除", false), nil
	}
	if m.BaseVersion != 0 && m.BaseVersion != cur.Version {
		return conflict(m, cur), nil
	}
	if _, err := tx.LockDailyStats(cur.SpaceID, cur.OccurredDate); err != nil {
		return MutationResult{}, err
	}
	t := now
	cur.DeletedAt = &t
	cur.Version++
	cur.UpdatedAt = now
	if err := tx.UpsertRecord(*cur); err != nil {
		return MutationResult{}, err
	}
	auth, err := s.recompute(tx, cur.SpaceID, now)
	if err != nil {
		return MutationResult{}, err
	}
	seq, err := tx.NextSeq()
	if err != nil {
		return MutationResult{}, err
	}
	_ = tx.InsertChange(Change{
		Sequence: seq, EntityType: "RECORD", EntityID: cur.ID, SpaceID: cur.SpaceID,
		Version: cur.Version, Op: "DELETE", Payload: map[string]any{"id": cur.ID, "version": cur.Version},
		ChangedAt: now,
	})
	return MutationResult{
		MutationID: m.MutationID, Status: "APPLIED", ClientLocalID: m.ClientLocalID,
		ServerID: cur.ID, Version: cur.Version, Authoritative: auth,
	}, nil
}

func (s *Service) applyUpsert(tx Tx, userID string, m Mutation, now time.Time) (MutationResult, error) {
	var cur *Record
	var err error
	if m.ServerID != "" {
		cur, err = tx.GetRecord(m.ServerID)
		if err != nil {
			return MutationResult{}, err
		}
		if cur == nil {
			return reject(m, "RECORD_INVALID", "记录不存在", false), nil
		}
		if cur.DeletedAt != nil {
			return reject(m, "RECORD_DELETED", "记录已删除", false), nil
		}
		if cur.AuthorID != userID {
			return reject(m, "SPACE_FORBIDDEN", "只有作者可以编辑", false), nil
		}
		if m.BaseVersion != cur.Version {
			return conflict(m, cur), nil
		}
	}

	mood := strings.TrimSpace(m.Payload.MoodTag)
	if _, ok := AllowedMoods[mood]; !ok {
		return reject(m, "RECORD_INVALID", "必须选择一个心情", false), nil
	}
	text := strings.TrimSpace(m.Payload.ContentText)
	if utf8.RuneCountInString(text) > 500 {
		return reject(m, "RECORD_INVALID", "文字不能超过 500 字", false), nil
	}
	tz := strings.TrimSpace(m.Timezone)
	if tz == "" {
		tz = "Asia/Shanghai"
	}
	loc, err := time.LoadLocation(tz)
	if err != nil {
		return reject(m, "RECORD_INVALID", "时区无效", false), nil
	}
	occurredAt := m.OccurredAt
	if occurredAt.IsZero() {
		occurredAt = now
	}
	if occurredAt.After(now.Add(5 * time.Minute)) {
		return reject(m, "RECORD_INVALID", "不能记录未来时间", false), nil
	}
	local := occurredAt.In(loc)
	date := m.OccurredDate
	if date == "" {
		date = local.Format("2006-01-02")
	}
	if local.Format("2006-01-02") != date {
		return reject(m, "RECORD_INVALID", "occurredDate 与时区不一致", false), nil
	}
	today := now.In(loc).Format("2006-01-02")
	oldest := now.In(loc).AddDate(0, 0, -365).Format("2006-01-02")
	if date < oldest {
		return reject(m, "RECORD_INVALID", "补记最多 365 天", false), nil
	}
	if date > today {
		return reject(m, "RECORD_INVALID", "不能记录未来日期", false), nil
	}

	spaceID := m.Payload.SpaceID
	createdAt := now
	recID := id.New()
	version := int64(1)
	if cur != nil {
		recID = cur.ID
		spaceID = cur.SpaceID
		createdAt = cur.CreatedAt
		version = cur.Version + 1
	} else if spaceID == "" {
		return reject(m, "RECORD_INVALID", "缺少 spaceId", false), nil
	}

	createdDate := createdAt.In(loc).Format("2006-01-02")
	isBackdated := date != createdDate
	if _, err := tx.LockDailyStats(spaceID, date); err != nil {
		return MutationResult{}, err
	}

	photos, voice, music, link, locn := countMedia(m.Payload.Media)
	var mediaIDs []string
	if s.Media != nil {
		in := make([]struct {
			MediaID string
			Type    string
		}, 0, len(m.Payload.Media))
		for _, mi := range m.Payload.Media {
			in = append(in, struct {
				MediaID string
				Type    string
			}{MediaID: mi.MediaID, Type: mi.Type})
		}
		pc, hv, ids, err := s.Media.Resolve(userID, recID, in)
		if err != nil {
			return reject(m, "RECORD_INVALID", "照片必须先上传完成再发布", false), nil
		}
		photos, voice, mediaIDs = pc, hv, ids
	}
	if photos > 9 {
		return reject(m, "RECORD_INVALID", "单条最多 9 张照片", false), nil
	}
	tags := m.Payload.StatusTags
	if len(tags) > 3 {
		tags = tags[:3]
	}

	live, err := tx.LiveRecords(spaceID)
	if err != nil {
		return MutationResult{}, err
	}
	dates := make([]string, 0, len(live)+1)
	todayGP := 0
	for _, r := range live {
		if cur != nil && r.ID == cur.ID {
			continue
		}
		dates = append(dates, r.OccurredDate)
		if r.OccurredDate == date {
			todayGP += r.GPFinal
		}
	}
	dates = append(dates, date)
	streak := growth.StreakEndingAt(dates, date)

	br := growth.Calculate(growth.Input{
		HasText: text != "", TextLength: utf8.RuneCountInString(text),
		PhotoCount: photos, HasVoice: voice, HasMusic: music, HasLink: link, HasLocation: locn,
		StatusTagCount: len(tags), StreakDays: streak, IsBackdated: isBackdated, TodayGpSoFar: todayGP,
	})

	rec := Record{
		ID: recID, ClientLocalID: m.ClientLocalID, SpaceID: spaceID, AuthorID: userID,
		ContentText: text, MoodTag: mood, StatusTags: tags,
		PhotoCount: photos, HasVoice: voice, HasMusic: music, HasLink: link, HasLocation: locn,
		OccurredAt: occurredAt.UTC(), OccurredDate: date, OccurredTimezone: tz,
		IsBackdated: isBackdated, GPFinal: br.FinalGP, GPCapped: br.IsCapped, GPBreakdown: br,
		Version: version, CreatedAt: createdAt, UpdatedAt: now,
	}
	if err := tx.UpsertRecord(rec); err != nil {
		return MutationResult{}, err
	}
	if s.Media != nil {
		if err := s.Media.Bind(rec.ID, mediaIDs); err != nil {
			return MutationResult{}, err
		}
	}
	auth, err := s.recompute(tx, spaceID, now)
	if err != nil {
		return MutationResult{}, err
	}
	if auth != nil {
		auth.GPFinal = rec.GPFinal
		auth.GPCapped = rec.GPCapped
	}
	seq, err := tx.NextSeq()
	if err != nil {
		return MutationResult{}, err
	}
	dto := rec.DTO()
	payload := map[string]any{}
	b, _ := json.Marshal(dto)
	_ = json.Unmarshal(b, &payload)
	_ = tx.InsertChange(Change{
		Sequence: seq, EntityType: "RECORD", EntityID: rec.ID, SpaceID: spaceID,
		Version: rec.Version, Op: "UPSERT", Payload: payload, ChangedAt: now,
	})
	return MutationResult{
		MutationID: m.MutationID, Status: "APPLIED", ClientLocalID: m.ClientLocalID,
		ServerID: rec.ID, Version: rec.Version, Authoritative: auth,
	}, nil
}

func (s *Service) recompute(tx Tx, spaceID string, now time.Time) (*Authoritative, error) {
	live, err := tx.LiveRecords(spaceID)
	if err != nil {
		return nil, err
	}
	var total int64
	byDate := map[string]*DailyStats{}
	dates := []string{}
	authors := map[string]map[string]struct{}{}
	for _, r := range live {
		total += int64(r.GPFinal)
		dates = append(dates, r.OccurredDate)
		st := byDate[r.OccurredDate]
		if st == nil {
			st = &DailyStats{SpaceID: spaceID, OccurredDate: r.OccurredDate}
			byDate[r.OccurredDate] = st
		}
		st.GPTotal += r.GPFinal
		st.RecordCount++
		if authors[r.OccurredDate] == nil {
			authors[r.OccurredDate] = map[string]struct{}{}
		}
		authors[r.OccurredDate][r.AuthorID] = struct{}{}
	}
	for d, st := range byDate {
		st.DistinctAuthorCount = len(authors[d])
		if st.GPTotal > growth.DailyGPLimit {
			st.GPTotal = growth.DailyGPLimit
		}
		if err := tx.SaveDailyStats(*st); err != nil {
			return nil, err
		}
	}
	stage := growth.StageFromGP(total)
	if err := tx.SetSpaceTotals(spaceID, total, stage); err != nil {
		return nil, err
	}
	sp, _ := tx.GetSpace(spaceID)
	tz := "Asia/Shanghai"
	if sp != nil && sp.Timezone != "" {
		tz = sp.Timezone
	}
	loc, err := time.LoadLocation(tz)
	if err != nil {
		loc = time.UTC
	}
	today := now.In(loc).Format("2006-01-02")
	streak := growth.DisplayStreak(dates, today)
	gpFinal := 0
	capped := false
	if len(live) > 0 {
		// last written isn't ordered; caller overwrites from the record itself
	}
	_ = capped
	return &Authoritative{
		GPFinal: gpFinal, SpaceTotalGP: total, PlantStage: stage, StreakDays: streak,
		UnlockedAchievements: []string{},
	}, nil
}

func (s *Service) Pull(userID, cursor string, limit int) (changes []Change, next string, hasMore bool, err error) {
	if limit <= 0 {
		limit = 100
	}
	if limit > 500 {
		limit = 500
	}
	after := parseCursor(cursor, userID)
	rows, err := s.Store.ListChanges(userID, after, limit+1)
	if err != nil {
		return nil, "", false, err
	}
	hasMore = len(rows) > limit
	if hasMore {
		rows = rows[:limit]
	}
	if len(rows) > 0 {
		next = formatCursor(userID, rows[len(rows)-1].Sequence)
	} else {
		next = cursor
	}
	return rows, next, hasMore, nil
}

func countMedia(media []MediaInput) (photos int, voice, music, link, loc bool) {
	for _, m := range media {
		switch strings.ToUpper(m.Type) {
		case "PHOTO":
			photos++
		case "VOICE":
			voice = true
		case "MUSIC":
			music = true
		case "LINK":
			link = true
		case "LOCATION":
			loc = true
		}
	}
	return
}

func reject(m Mutation, code, msg string, retry bool) MutationResult {
	status := "REJECTED"
	if retry {
		status = "RETRYABLE"
	}
	if code == "RECORD_VERSION_CONFLICT" {
		status = "CONFLICT"
	}
	return MutationResult{
		MutationID: m.MutationID, Status: status, ClientLocalID: m.ClientLocalID, ServerID: m.ServerID,
		Error: &ResultError{Code: code, Message: msg, Retryable: retry},
	}
}

func conflict(m Mutation, cur *Record) MutationResult {
	res := reject(m, "RECORD_VERSION_CONFLICT", "记录已在其他设备更新", false)
	res.Status = "CONFLICT"
	res.ServerID = cur.ID
	res.Version = cur.Version
	return res
}

func formatCursor(userID string, seq int64) string {
	return fmt.Sprintf("v1.%s.%d", userID, seq)
}

func parseCursor(cursor, userID string) int64 {
	if cursor == "" {
		return 0
	}
	parts := strings.Split(cursor, ".")
	if len(parts) >= 3 && parts[0] == "v1" {
		if parts[1] != userID {
			return 0
		}
		var seq int64
		fmt.Sscanf(parts[len(parts)-1], "%d", &seq)
		return seq
	}
	var only int64
	fmt.Sscanf(cursor, "%d", &only)
	return only
}
