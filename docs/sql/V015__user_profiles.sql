-- V015__user_profiles.sql
-- Tabla de perfiles de usuario (display name + avatar)
create table if not exists user_profiles (
    user_id      uuid primary key references auth.users(id) on delete cascade,
    display_name text not null default '',
    avatar_url   text,
    updated_at   timestamptz not null default now()
);

-- RLS
alter table user_profiles enable row level security;
create policy "user_profiles_select" on user_profiles for select using (true);
create policy "user_profiles_insert" on user_profiles for insert with check (auth.uid() = user_id);
create policy "user_profiles_update" on user_profiles for update using (auth.uid() = user_id);

comment on table user_profiles is 'Perfil público del usuario (nombre visible y avatar)';

