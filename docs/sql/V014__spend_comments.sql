-- V014__spend_comments.sql
create table if not exists spend_comments (
    id             bigint generated always as identity primary key,
    spend_id       bigint references spends(id) on delete cascade not null,
    participant_id bigint references participants(id) on delete cascade not null,
    text           text not null check (char_length(text) <= 500),
    created_at     timestamptz not null default now()
);

create index if not exists spend_comments_spend_id_idx on spend_comments (spend_id);

-- RLS
alter table spend_comments enable row level security;
create policy "spend_comments_select" on spend_comments for select using (true);
create policy "spend_comments_insert" on spend_comments for insert with check (true);
create policy "spend_comments_delete" on spend_comments for delete using (true);

comment on table spend_comments is 'Hilo de comentarios por gasto';

