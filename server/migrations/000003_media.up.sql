create table if not exists media_objects (
  id uuid primary key,
  user_id uuid not null references users(id),
  record_id uuid null references records(id),
  type varchar(20) not null,
  object_key text not null,
  upload_status varchar(20) not null,
  mime_type varchar(100) not null,
  size_bytes bigint not null default 0,
  reserved_bytes bigint not null default 0,
  sha256 varchar(64) null,
  width int not null default 0,
  height int not null default 0,
  duration_ms int not null default 0,
  deleted_at timestamptz null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint media_objects_type_chk check (type in ('PHOTO','VOICE')),
  constraint media_objects_status_chk check (upload_status in ('PENDING','READY','MISSING','DELETED')),
  constraint media_objects_key_uq unique (object_key)
);
create index if not exists media_objects_user_idx on media_objects (user_id) where deleted_at is null;
create index if not exists media_objects_record_idx on media_objects (record_id) where record_id is not null and deleted_at is null;
