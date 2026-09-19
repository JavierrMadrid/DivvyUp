-- V013__activity_log.sql
create table if not exists activity_log (
    id                   bigint generated always as identity primary key,
    group_id             bigint references groups(id) on delete cascade not null,
    actor_participant_id bigint references participants(id) on delete set null,
    actor_name           text,
    event_type           text not null,
    entity_id            bigint,
    description          text not null default '',
    created_at           timestamptz not null default now()
);

create index if not exists activity_log_group_id_idx  on activity_log (group_id);
create index if not exists activity_log_created_at_idx on activity_log (group_id, created_at desc);

-- RLS
alter table activity_log enable row level security;
create policy "activity_log_select" on activity_log for select using (true);
create policy "activity_log_insert" on activity_log for insert with check (true);

comment on table activity_log is 'Feed de eventos del grupo (gastos, liquidaciones, participantes)';

