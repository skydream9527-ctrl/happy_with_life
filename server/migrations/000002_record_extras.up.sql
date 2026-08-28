alter table records add column if not exists status_tags jsonb not null default '[]'::jsonb;
alter table records add column if not exists media_flags jsonb not null default '{}'::jsonb;
