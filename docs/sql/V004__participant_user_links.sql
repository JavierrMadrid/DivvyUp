-- =============================================================================
-- DivvyUp — V004
-- 1) Añade owner_user_id a groups (propietario del grupo)
-- 2) Tabla participant_user_links (vínculo usuario ↔ participante)
-- 3) Tabla group_invite_tokens (tokens de invitación con TTL 7 días)
-- =============================================================================

-- ── 1. owner_user_id en groups ───────────────────────────────────────────────
alter table groups
  add column if not exists owner_user_id uuid references auth.users(id) on delete set null;

create index if not exists groups_owner_user_id_idx on groups (owner_user_id);

-- ── 2. participant_user_links ────────────────────────────────────────────────
-- Un usuario autenticado se vincula a exactamente un participante por grupo.
-- UNIQUE (group_id, user_id)  → un usuario, un participante por grupo.
-- UNIQUE (participant_id)     → cada participante tiene como máximo un usuario.

create table if not exists participant_user_links (
  id             bigint generated always as identity primary key,
  group_id       bigint  not null references groups(id)       on delete cascade,
  participant_id bigint  not null references participants(id) on delete cascade,
  user_id        uuid    not null references auth.users(id)   on delete cascade,
  created_at     timestamptz not null default now(),
  unique (group_id, user_id),
  unique (participant_id)
);

create index if not exists participant_user_links_group_id_idx on participant_user_links (group_id);
create index if not exists participant_user_links_user_id_idx  on participant_user_links (user_id);

alter table participant_user_links enable row level security;

create policy "anon_select" on participant_user_links for select to anon using (true);
create policy "anon_insert" on participant_user_links for insert to anon with check (true);
create policy "anon_update" on participant_user_links for update to anon using (true) with check (true);
create policy "anon_delete" on participant_user_links for delete to anon using (true);

-- ── 3. group_invite_tokens ───────────────────────────────────────────────────
-- Cada fila es un token UUID válido durante 7 días.
-- El propietario puede generar varios tokens (uno por sesión de compartir).
-- Un token es válido si expires_at > now().

create table if not exists group_invite_tokens (
  token              uuid        not null default gen_random_uuid() primary key,
  group_id           bigint      not null references groups(id) on delete cascade,
  created_by_user_id uuid        not null references auth.users(id) on delete cascade,
  expires_at         timestamptz not null default (now() + interval '7 days'),
  created_at         timestamptz not null default now()
);

create index if not exists group_invite_tokens_group_id_idx    on group_invite_tokens (group_id);
create index if not exists group_invite_tokens_expires_at_idx  on group_invite_tokens (expires_at);

alter table group_invite_tokens enable row level security;

create policy "anon_select" on group_invite_tokens for select to anon using (true);
create policy "anon_insert" on group_invite_tokens for insert to anon with check (true);
create policy "anon_delete" on group_invite_tokens for delete to anon using (true);

comment on table group_invite_tokens
  is 'Tokens UUID de invitación a grupos. Válidos 7 días desde su creación.';
comment on column group_invite_tokens.expires_at
  is 'Fecha de expiración. El token no es válido si expires_at <= now().';
