package ledger

import (
	"context"
	"encoding/json"
	"errors"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/skydream9527-ctrl/xiaoquexing-server/internal/growth"
	"github.com/skydream9527-ctrl/xiaoquexing-server/internal/id"
)

type PG struct {
	pool *pgxpool.Pool
}

func NewPG(pool *pgxpool.Pool) *PG { return &PG{pool: pool} }

func (p *PG) ctx() (context.Context, context.CancelFunc) {
	return context.WithTimeout(context.Background(), 8*time.Second)
}

func (p *PG) EnsurePersonalSpace(userID, name string) (Space, error) {
	ctx, cancel := p.ctx()
	defer cancel()
	row := p.pool.QueryRow(ctx, `select id, name, space_type, owner_id, total_gp, active_plant_type, timezone, version, created_at
		from spaces where owner_id=$1 and space_type='PERSONAL' and deleted_at is null limit 1`, userID)
	sp, err := scanSpace(row)
	if err == nil {
		return sp, nil
	}
	if !errors.Is(err, pgx.ErrNoRows) {
		return Space{}, err
	}
	now := time.Now().UTC()
	sp = Space{
		ID: id.New(), Name: name, SpaceType: "PERSONAL", OwnerID: userID,
		ActivePlantType: "TREE", PlantStage: growth.StageFromGP(0), Timezone: "Asia/Shanghai",
		Version: 1, CreatedAt: now,
	}
	tx, err := p.pool.Begin(ctx)
	if err != nil {
		return Space{}, err
	}
	defer tx.Rollback(ctx)
	_, err = tx.Exec(ctx, `insert into spaces (id, name, space_type, owner_id, total_gp, active_plant_type, timezone, version, created_at, updated_at)
		values ($1,$2,$3,$4,0,$5,$6,1,$7,$7)`, sp.ID, sp.Name, sp.SpaceType, sp.OwnerID, sp.ActivePlantType, sp.Timezone, now)
	if err != nil {
		return Space{}, err
	}
	_, err = tx.Exec(ctx, `insert into space_members (space_id, user_id, role, status, joined_at, created_at, updated_at)
		values ($1,$2,'OWNER','ACTIVE',$3,$3,$3)`, sp.ID, userID, now)
	if err != nil {
		return Space{}, err
	}
	return sp, tx.Commit(ctx)
}

func (p *PG) ListSpacesForUser(userID string) ([]Space, error) {
	ctx, cancel := p.ctx()
	defer cancel()
	rows, err := p.pool.Query(ctx, `select s.id, s.name, s.space_type, s.owner_id, s.total_gp, s.active_plant_type, s.timezone, s.version, s.created_at
		from spaces s join space_members m on m.space_id=s.id
		where m.user_id=$1 and m.status='ACTIVE' and s.deleted_at is null`, userID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []Space{}
	for rows.Next() {
		sp, err := scanSpace(rows)
		if err != nil {
			return nil, err
		}
		out = append(out, sp)
	}
	return out, rows.Err()
}

func (p *PG) GetSpace(id string) (*Space, error) {
	ctx, cancel := p.ctx()
	defer cancel()
	sp, err := scanSpace(p.pool.QueryRow(ctx, `select id, name, space_type, owner_id, total_gp, active_plant_type, timezone, version, created_at from spaces where id=$1`, id))
	if errors.Is(err, pgx.ErrNoRows) {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	return &sp, nil
}

func (p *PG) GetMember(spaceID, userID string) (*Member, error) {
	ctx, cancel := p.ctx()
	defer cancel()
	var m Member
	err := p.pool.QueryRow(ctx, `select space_id, user_id, role, status, contributed_gp from space_members where space_id=$1 and user_id=$2`, spaceID, userID).
		Scan(&m.SpaceID, &m.UserID, &m.Role, &m.Status, &m.ContributedGP)
	if errors.Is(err, pgx.ErrNoRows) {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	return &m, nil
}

func (p *PG) GetRecord(id string) (*Record, error) {
	ctx, cancel := p.ctx()
	defer cancel()
	r, err := scanRecord(p.pool.QueryRow(ctx, recordSelect+" where id=$1", id))
	if errors.Is(err, pgx.ErrNoRows) {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	return &r, nil
}

func (p *PG) LiveRecords(spaceID string) ([]Record, error) {
	ctx, cancel := p.ctx()
	defer cancel()
	rows, err := p.pool.Query(ctx, recordSelect+" where space_id=$1 and deleted_at is null", spaceID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []Record{}
	for rows.Next() {
		r, err := scanRecord(rows)
		if err != nil {
			return nil, err
		}
		out = append(out, r)
	}
	return out, rows.Err()
}

func (p *PG) ListRecords(spaceID, afterOccurred, afterID string, limit int) ([]Record, error) {
	ctx, cancel := p.ctx()
	defer cancel()
	rows, err := p.pool.Query(ctx, recordSelect+`
		where space_id=$1 and deleted_at is null
		order by occurred_at desc, id desc limit $2`, spaceID, limit+50)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []Record{}
	for rows.Next() {
		r, err := scanRecord(rows)
		if err != nil {
			return nil, err
		}
		key := r.OccurredAt.UTC().Format(time.RFC3339Nano) + "|" + r.ID
		if afterOccurred != "" && key >= afterOccurred+"|"+afterID {
			continue
		}
		out = append(out, r)
		if len(out) >= limit {
			break
		}
	}
	return out, rows.Err()
}

func (p *PG) ListChanges(userID string, afterSeq int64, limit int) ([]Change, error) {
	ctx, cancel := p.ctx()
	defer cancel()
	rows, err := p.pool.Query(ctx, `select c.sequence, c.entity_type, c.entity_id, c.space_id, c.version, c.op, c.payload, c.changed_at
		from change_log c
		join space_members m on m.space_id=c.space_id
		where m.user_id=$1 and m.status='ACTIVE' and c.sequence>$2
		order by c.sequence asc limit $3`, userID, afterSeq, limit)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []Change{}
	for rows.Next() {
		var c Change
		var payload []byte
		if err := rows.Scan(&c.Sequence, &c.EntityType, &c.EntityID, &c.SpaceID, &c.Version, &c.Op, &payload, &c.ChangedAt); err != nil {
			return nil, err
		}
		_ = json.Unmarshal(payload, &c.Payload)
		out = append(out, c)
	}
	return out, rows.Err()
}

func (p *PG) ApplyTx(fn func(tx Tx) error) error {
	ctx, cancel := p.ctx()
	defer cancel()
	tx, err := p.pool.Begin(ctx)
	if err != nil {
		return err
	}
	defer tx.Rollback(ctx)
	if err := fn(&pgTx{ctx: ctx, tx: tx}); err != nil {
		return err
	}
	return tx.Commit(ctx)
}

type pgTx struct {
	ctx context.Context
	tx  pgx.Tx
}

func (t *pgTx) GetMember(spaceID, userID string) (*Member, error) {
	var m Member
	err := t.tx.QueryRow(t.ctx, `select space_id, user_id, role, status, contributed_gp from space_members where space_id=$1 and user_id=$2`, spaceID, userID).
		Scan(&m.SpaceID, &m.UserID, &m.Role, &m.Status, &m.ContributedGP)
	if errors.Is(err, pgx.ErrNoRows) {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	return &m, nil
}
func (t *pgTx) GetSpace(id string) (*Space, error) {
	sp, err := scanSpace(t.tx.QueryRow(t.ctx, `select id, name, space_type, owner_id, total_gp, active_plant_type, timezone, version, created_at from spaces where id=$1`, id))
	if errors.Is(err, pgx.ErrNoRows) {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	return &sp, nil
}
func (t *pgTx) GetRecord(id string) (*Record, error) {
	r, err := scanRecord(t.tx.QueryRow(t.ctx, recordSelect+" where id=$1", id))
	if errors.Is(err, pgx.ErrNoRows) {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	return &r, nil
}
func (t *pgTx) LiveRecords(spaceID string) ([]Record, error) {
	rows, err := t.tx.Query(t.ctx, recordSelect+" where space_id=$1 and deleted_at is null", spaceID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []Record{}
	for rows.Next() {
		r, err := scanRecord(rows)
		if err != nil {
			return nil, err
		}
		out = append(out, r)
	}
	return out, rows.Err()
}
func (t *pgTx) UpsertRecord(r Record) error {
	br, _ := json.Marshal(r.GPBreakdown)
	tags, _ := json.Marshal(r.StatusTags)
	flags, _ := json.Marshal(map[string]any{"photoCount": r.PhotoCount, "hasVoice": r.HasVoice, "hasMusic": r.HasMusic, "hasLink": r.HasLink, "hasLocation": r.HasLocation})
	_, err := t.tx.Exec(t.ctx, `insert into records (
		id, client_local_id, space_id, author_id, content_text, mood_tag, occurred_at, occurred_date, occurred_timezone,
		is_backdated, gp_final, gp_capped, gp_breakdown, version, deleted_at, created_at, updated_at, status_tags, media_flags)
		values ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15,$16,$17,$18,$19)
		on conflict (id) do update set
			content_text=excluded.content_text, mood_tag=excluded.mood_tag, occurred_at=excluded.occurred_at,
			occurred_date=excluded.occurred_date, occurred_timezone=excluded.occurred_timezone, is_backdated=excluded.is_backdated,
			gp_final=excluded.gp_final, gp_capped=excluded.gp_capped, gp_breakdown=excluded.gp_breakdown, version=excluded.version,
			deleted_at=excluded.deleted_at, updated_at=excluded.updated_at, status_tags=excluded.status_tags, media_flags=excluded.media_flags`,
		r.ID, r.ClientLocalID, r.SpaceID, r.AuthorID, nullIfEmpty(r.ContentText), r.MoodTag, r.OccurredAt, r.OccurredDate, r.OccurredTimezone,
		r.IsBackdated, r.GPFinal, r.GPCapped, br, r.Version, r.DeletedAt, r.CreatedAt, r.UpdatedAt, tags, flags)
	return err
}
func (t *pgTx) LockDailyStats(spaceID, date string) (DailyStats, error) {
	_, _ = t.tx.Exec(t.ctx, `insert into daily_space_stats (space_id, occurred_date, gp_total, record_count, distinct_author_count)
		values ($1,$2,0,0,0) on conflict do nothing`, spaceID, date)
	var s DailyStats
	err := t.tx.QueryRow(t.ctx, `select space_id, occurred_date, gp_total, record_count, distinct_author_count
		from daily_space_stats where space_id=$1 and occurred_date=$2::date for update`, spaceID, date).
		Scan(&s.SpaceID, &s.OccurredDate, &s.GPTotal, &s.RecordCount, &s.DistinctAuthorCount)
	return s, err
}
func (t *pgTx) SaveDailyStats(s DailyStats) error {
	_, err := t.tx.Exec(t.ctx, `insert into daily_space_stats (space_id, occurred_date, gp_total, record_count, distinct_author_count, updated_at)
		values ($1,$2,$3,$4,$5,now())
		on conflict (space_id, occurred_date) do update set gp_total=$3, record_count=$4, distinct_author_count=$5, updated_at=now()`,
		s.SpaceID, s.OccurredDate, s.GPTotal, s.RecordCount, s.DistinctAuthorCount)
	return err
}
func (t *pgTx) SetSpaceTotals(spaceID string, totalGP int64, stage string) error {
	_, err := t.tx.Exec(t.ctx, `update spaces set total_gp=$2, updated_at=now(), version=version+1 where id=$1`, spaceID, totalGP)
	return err
}
func (t *pgTx) NextSeq() (int64, error) {
	var seq int64
	err := t.tx.QueryRow(t.ctx, `select nextval('change_log_sequence_seq')`).Scan(&seq)
	return seq, err
}
func (t *pgTx) InsertChange(c Change) error {
	payload, _ := json.Marshal(c.Payload)
	_, err := t.tx.Exec(t.ctx, `insert into change_log (sequence, entity_type, entity_id, space_id, version, op, payload, changed_at)
		values ($1,$2,$3,$4,$5,$6,$7,$8)`, c.Sequence, c.EntityType, c.EntityID, c.SpaceID, c.Version, c.Op, payload, c.ChangedAt)
	return err
}
func (t *pgTx) GetMutation(id string) (*AppliedMutation, error) {
	var m AppliedMutation
	err := t.tx.QueryRow(t.ctx, `select mutation_id, user_id, coalesce(device_id::text,''), request_hash, response_json from applied_mutations where mutation_id=$1`, id).
		Scan(&m.MutationID, &m.UserID, &m.DeviceID, &m.RequestHash, &m.ResponseJSON)
	if errors.Is(err, pgx.ErrNoRows) {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	return &m, nil
}
func (t *pgTx) SaveMutation(m AppliedMutation) error {
	_, err := t.tx.Exec(t.ctx, `insert into applied_mutations (mutation_id, user_id, device_id, request_hash, response_json)
		values ($1,$2,nullif($3,'')::uuid,$4,$5)
		on conflict (mutation_id) do nothing`, m.MutationID, m.UserID, m.DeviceID, m.RequestHash, m.ResponseJSON)
	return err
}

func (p *PG) CreateSpace(sp Space, owner Member) error {
	ctx, cancel := p.ctx()
	defer cancel()
	tx, err := p.pool.Begin(ctx)
	if err != nil {
		return err
	}
	defer tx.Rollback(ctx)
	_, err = tx.Exec(ctx, `insert into spaces (id, name, space_type, owner_id, total_gp, active_plant_type, timezone, version, created_at, updated_at)
		values ($1,$2,$3,$4,0,$5,$6,1,$7,$7)`,
		sp.ID, sp.Name, sp.SpaceType, sp.OwnerID, sp.ActivePlantType, sp.Timezone, sp.CreatedAt)
	if err != nil {
		return err
	}
	_, err = tx.Exec(ctx, `insert into space_members (space_id, user_id, role, status, joined_at, created_at, updated_at)
		values ($1,$2,$3,'ACTIVE',$4,$4,$4)`, sp.ID, owner.UserID, owner.Role, sp.CreatedAt)
	if err != nil {
		return err
	}
	return tx.Commit(ctx)
}

func (p *PG) UpdateSpace(sp Space) error {
	ctx, cancel := p.ctx()
	defer cancel()
	_, err := p.pool.Exec(ctx, `update spaces set name=$2, active_plant_type=$3, timezone=$4, version=version+1, updated_at=now() where id=$1`,
		sp.ID, sp.Name, sp.ActivePlantType, sp.Timezone)
	return err
}

func (p *PG) ListMembers(spaceID string) ([]Member, error) {
	ctx, cancel := p.ctx()
	defer cancel()
	rows, err := p.pool.Query(ctx, `select space_id, user_id, role, status, contributed_gp from space_members where space_id=$1`, spaceID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []Member{}
	for rows.Next() {
		var m Member
		if err := rows.Scan(&m.SpaceID, &m.UserID, &m.Role, &m.Status, &m.ContributedGP); err != nil {
			return nil, err
		}
		out = append(out, m)
	}
	return out, rows.Err()
}

func (p *PG) SaveMember(m Member) error {
	ctx, cancel := p.ctx()
	defer cancel()
	_, err := p.pool.Exec(ctx, `insert into space_members (space_id, user_id, role, status, contributed_gp, joined_at, created_at, updated_at)
		values ($1,$2,$3,$4,$5,now(),now(),now())
		on conflict (space_id, user_id) do update set role=excluded.role, status=excluded.status, contributed_gp=excluded.contributed_gp, left_at=case when excluded.status='ACTIVE' then null else now() end, updated_at=now()`,
		m.SpaceID, m.UserID, m.Role, m.Status, m.ContributedGP)
	return err
}

func (p *PG) CountActiveMembers(spaceID string) (int, error) {
	ctx, cancel := p.ctx()
	defer cancel()
	var n int
	err := p.pool.QueryRow(ctx, `select count(*) from space_members where space_id=$1 and status='ACTIVE'`, spaceID).Scan(&n)
	return n, err
}

func (p *PG) InsertInvite(inv Invite) error {
	ctx, cancel := p.ctx()
	defer cancel()
	_, err := p.pool.Exec(ctx, `insert into space_invites (id, space_id, inviter_id, token_hash, expires_at, max_uses, used_count, created_at, updated_at)
		values ($1,$2,$3,$4,$5,$6,$7,$8,$8)`,
		inv.ID, inv.SpaceID, inv.InviterID, inv.TokenHash, inv.ExpiresAt, inv.MaxUses, inv.UsedCount, inv.CreatedAt)
	return err
}

func (p *PG) GetInviteByHash(hash string) (*Invite, error) {
	ctx, cancel := p.ctx()
	defer cancel()
	inv, err := scanInvite(p.pool.QueryRow(ctx, inviteSelect+" where token_hash=$1", hash))
	if errors.Is(err, pgx.ErrNoRows) {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	return &inv, nil
}

func (p *PG) GetInvite(id string) (*Invite, error) {
	ctx, cancel := p.ctx()
	defer cancel()
	inv, err := scanInvite(p.pool.QueryRow(ctx, inviteSelect+" where id=$1", id))
	if errors.Is(err, pgx.ErrNoRows) {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	return &inv, nil
}

func (p *PG) UpdateInvite(inv Invite) error {
	ctx, cancel := p.ctx()
	defer cancel()
	_, err := p.pool.Exec(ctx, `update space_invites set used_count=$2, revoked_at=$3, updated_at=now() where id=$1`,
		inv.ID, inv.UsedCount, inv.RevokedAt)
	return err
}

func (p *PG) ListInvites(spaceID string) ([]Invite, error) {
	ctx, cancel := p.ctx()
	defer cancel()
	rows, err := p.pool.Query(ctx, inviteSelect+" where space_id=$1 order by created_at desc", spaceID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []Invite{}
	for rows.Next() {
		inv, err := scanInvite(rows)
		if err != nil {
			return nil, err
		}
		out = append(out, inv)
	}
	return out, rows.Err()
}

const inviteSelect = `select id, space_id, inviter_id, token_hash, expires_at, max_uses, used_count, revoked_at, created_at from space_invites`

func scanInvite(row rowScanner) (Invite, error) {
	var inv Invite
	err := row.Scan(&inv.ID, &inv.SpaceID, &inv.InviterID, &inv.TokenHash, &inv.ExpiresAt, &inv.MaxUses, &inv.UsedCount, &inv.RevokedAt, &inv.CreatedAt)
	return inv, err
}

const recordSelect = `select id, client_local_id, space_id, author_id, coalesce(content_text,''), mood_tag, occurred_at, occurred_date::text, occurred_timezone,
	is_backdated, gp_final, gp_capped, gp_breakdown, version, deleted_at, created_at, updated_at, coalesce(status_tags,'[]'::jsonb), coalesce(media_flags,'{}'::jsonb)
	from records`

type rowScanner interface {
	Scan(dest ...any) error
}

func scanSpace(row rowScanner) (Space, error) {
	var s Space
	err := row.Scan(&s.ID, &s.Name, &s.SpaceType, &s.OwnerID, &s.TotalGP, &s.ActivePlantType, &s.Timezone, &s.Version, &s.CreatedAt)
	s.PlantStage = growth.StageFromGP(s.TotalGP)
	return s, err
}

func scanRecord(row rowScanner) (Record, error) {
	var r Record
	var br, tags, flags []byte
	err := row.Scan(&r.ID, &r.ClientLocalID, &r.SpaceID, &r.AuthorID, &r.ContentText, &r.MoodTag, &r.OccurredAt, &r.OccurredDate, &r.OccurredTimezone,
		&r.IsBackdated, &r.GPFinal, &r.GPCapped, &br, &r.Version, &r.DeletedAt, &r.CreatedAt, &r.UpdatedAt, &tags, &flags)
	_ = json.Unmarshal(br, &r.GPBreakdown)
	_ = json.Unmarshal(tags, &r.StatusTags)
	var mf struct {
		PhotoCount  int  `json:"photoCount"`
		HasVoice    bool `json:"hasVoice"`
		HasMusic    bool `json:"hasMusic"`
		HasLink     bool `json:"hasLink"`
		HasLocation bool `json:"hasLocation"`
	}
	_ = json.Unmarshal(flags, &mf)
	r.PhotoCount, r.HasVoice, r.HasMusic, r.HasLink, r.HasLocation = mf.PhotoCount, mf.HasVoice, mf.HasMusic, mf.HasLink, mf.HasLocation
	return r, err
}

func nullIfEmpty(s string) any {
	if s == "" {
		return nil
	}
	return s
}
