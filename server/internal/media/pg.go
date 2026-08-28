package media

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

func (p *PGStore) ctx() (context.Context, context.CancelFunc) {
	return context.WithTimeout(context.Background(), 8*time.Second)
}

func (p *PGStore) Insert(o Object) error {
	ctx, cancel := p.ctx()
	defer cancel()
	_, err := p.pool.Exec(ctx, `insert into media_objects
		(id, user_id, record_id, type, object_key, upload_status, mime_type, size_bytes, reserved_bytes, sha256, width, height, duration_ms, created_at, updated_at)
		values ($1,$2,nullif($3,''),$4,$5,$6,$7,$8,$9,nullif($10,''),$11,$12,$13,$14,$14)`,
		o.ID, o.UserID, o.RecordID, o.Type, o.ObjectKey, o.UploadStatus, o.MimeType,
		o.SizeBytes, o.ReservedBytes, o.SHA256, o.Width, o.Height, o.DurationMS, o.CreatedAt)
	return err
}

func (p *PGStore) Update(o Object) error {
	ctx, cancel := p.ctx()
	defer cancel()
	_, err := p.pool.Exec(ctx, `update media_objects set
		record_id=nullif($2,''), upload_status=$3, size_bytes=$4, reserved_bytes=$5,
		sha256=nullif($6,''), width=$7, height=$8, duration_ms=$9, deleted_at=$10, updated_at=$11
		where id=$1`,
		o.ID, o.RecordID, o.UploadStatus, o.SizeBytes, o.ReservedBytes, o.SHA256,
		o.Width, o.Height, o.DurationMS, o.DeletedAt, o.UpdatedAt)
	return err
}

func (p *PGStore) Get(id string) (*Object, error) {
	ctx, cancel := p.ctx()
	defer cancel()
	row := p.pool.QueryRow(ctx, `select id, user_id, coalesce(record_id::text,''), type, object_key, upload_status,
		mime_type, size_bytes, reserved_bytes, coalesce(sha256,''), width, height, duration_ms, deleted_at, created_at, updated_at
		from media_objects where id=$1`, id)
	o, err := scanObject(row)
	if errors.Is(err, pgx.ErrNoRows) {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	return &o, nil
}

func (p *PGStore) ListByRecord(recordID string) ([]Object, error) {
	ctx, cancel := p.ctx()
	defer cancel()
	rows, err := p.pool.Query(ctx, `select id, user_id, coalesce(record_id::text,''), type, object_key, upload_status,
		mime_type, size_bytes, reserved_bytes, coalesce(sha256,''), width, height, duration_ms, deleted_at, created_at, updated_at
		from media_objects where record_id=$1 and deleted_at is null and upload_status <> 'DELETED' order by created_at`, recordID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []Object{}
	for rows.Next() {
		o, err := scanObject(rows)
		if err != nil {
			return nil, err
		}
		out = append(out, o)
	}
	return out, rows.Err()
}

func (p *PGStore) Usage(userID string) (int64, error) {
	ctx, cancel := p.ctx()
	defer cancel()
	var n int64
	err := p.pool.QueryRow(ctx, `select coalesce(sum(
		case when upload_status='PENDING' then reserved_bytes
		     when upload_status='READY' then size_bytes
		     else 0 end),0)
		from media_objects where user_id=$1 and deleted_at is null and upload_status in ('PENDING','READY')`, userID).Scan(&n)
	return n, err
}

type scanner interface {
	Scan(dest ...any) error
}

func scanObject(row scanner) (Object, error) {
	var o Object
	err := row.Scan(&o.ID, &o.UserID, &o.RecordID, &o.Type, &o.ObjectKey, &o.UploadStatus,
		&o.MimeType, &o.SizeBytes, &o.ReservedBytes, &o.SHA256, &o.Width, &o.Height, &o.DurationMS,
		&o.DeletedAt, &o.CreatedAt, &o.UpdatedAt)
	return o, err
}
