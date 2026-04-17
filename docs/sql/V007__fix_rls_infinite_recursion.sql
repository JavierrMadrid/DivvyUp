-- =============================================================================
-- DivvyUp — V007
-- Corrige la recursión infinita en las políticas RLS de V006.
--
-- PROBLEMA:
--   groups_select  → consulta participant_user_links
--                  → activa pul_select
--                  → consulta groups
--                  → activa groups_select → ∞
--
--   pul_select     → consulta participant_user_links pul2 (auto-referencia) → ∞
--
-- SOLUCIÓN:
--   Dos funciones SECURITY DEFINER que leen las tablas bypassando RLS,
--   rompiendo los ciclos. Todas las policies que causaban el ciclo son
--   reemplazadas por versiones que usan estas funciones.
-- =============================================================================

-- ─────────────────────────────────────────────────────────────────────────────
-- HELPER 1: comprueba si el usuario actual es dueño del grupo
--           (lee groups sin activar su RLS policy)
-- ─────────────────────────────────────────────────────────────────────────────
create or replace function is_group_owner(p_group_id bigint)
returns boolean
language sql
security definer
stable
set search_path = public
as $$
  select exists (
    select 1 from groups
    where id = p_group_id
      and owner_user_id = auth.uid()
  );
$$;

-- ─────────────────────────────────────────────────────────────────────────────
-- HELPER 2: comprueba si el usuario actual tiene un vínculo en el grupo
--           (lee participant_user_links sin activar su RLS policy)
-- ─────────────────────────────────────────────────────────────────────────────
create or replace function is_group_member(p_group_id bigint)
returns boolean
language sql
security definer
stable
set search_path = public
as $$
  select exists (
    select 1 from participant_user_links
    where group_id = p_group_id
      and user_id  = auth.uid()
  );
$$;

-- ─────────────────────────────────────────────────────────────────────────────
-- 1. groups — reemplazar policies que referenciaban participant_user_links
-- ─────────────────────────────────────────────────────────────────────────────
drop policy if exists "groups_select" on groups;
drop policy if exists "groups_insert" on groups;
drop policy if exists "groups_update" on groups;
drop policy if exists "groups_delete" on groups;

create policy "groups_select" on groups
  for select to authenticated
  using (
    owner_user_id = (select auth.uid())
    or is_group_member(id)
  );

create policy "groups_insert" on groups
  for insert to authenticated
  with check (
    owner_user_id = (select auth.uid())
  );

create policy "groups_update" on groups
  for update to authenticated
  using  (owner_user_id = (select auth.uid()))
  with check (owner_user_id = (select auth.uid()));

create policy "groups_delete" on groups
  for delete to authenticated
  using (owner_user_id = (select auth.uid()));

-- ─────────────────────────────────────────────────────────────────────────────
-- 2. participant_user_links — reemplazar policies recursivas
-- ─────────────────────────────────────────────────────────────────────────────
drop policy if exists "pul_select" on participant_user_links;
drop policy if exists "pul_insert" on participant_user_links;
drop policy if exists "pul_delete" on participant_user_links;

-- SELECT: el propio usuario ve sus vínculos; el dueño del grupo ve todos
create policy "pul_select" on participant_user_links
  for select to authenticated
  using (
    user_id = (select auth.uid())
    or is_group_owner(group_id)
  );

-- INSERT: solo el propio usuario puede vincularse a sí mismo
create policy "pul_insert" on participant_user_links
  for insert to authenticated
  with check (
    user_id = (select auth.uid())
  );

-- DELETE: el propio usuario puede desvincularse, o el dueño del grupo
create policy "pul_delete" on participant_user_links
  for delete to authenticated
  using (
    user_id = (select auth.uid())
    or is_group_owner(group_id)
  );

-- ─────────────────────────────────────────────────────────────────────────────
-- 3. participants — reemplazar policies que referenciaban participant_user_links
--    a través de groups (el ciclo pasaba por aquí también)
-- ─────────────────────────────────────────────────────────────────────────────
drop policy if exists "participants_select" on participants;
drop policy if exists "participants_insert" on participants;
drop policy if exists "participants_update" on participants;
drop policy if exists "participants_delete" on participants;

create policy "participants_select" on participants
  for select to authenticated
  using (
    is_group_owner(group_id)
    or is_group_member(group_id)
  );

create policy "participants_insert" on participants
  for insert to authenticated
  with check (
    is_group_owner(group_id)
    or is_group_member(group_id)
  );

create policy "participants_update" on participants
  for update to authenticated
  using (is_group_owner(group_id) or is_group_member(group_id))
  with check (is_group_owner(group_id) or is_group_member(group_id));

create policy "participants_delete" on participants
  for delete to authenticated
  using (
    is_group_owner(group_id)
    or is_group_member(group_id)
  );

-- ─────────────────────────────────────────────────────────────────────────────
-- 4. categories — reemplazar con helpers
-- ─────────────────────────────────────────────────────────────────────────────
drop policy if exists "categories_select" on categories;
drop policy if exists "categories_insert" on categories;
drop policy if exists "categories_update" on categories;
drop policy if exists "categories_delete" on categories;

create policy "categories_select" on categories
  for select to authenticated
  using (
    group_id is null
    or is_group_owner(group_id)
    or is_group_member(group_id)
  );

create policy "categories_insert" on categories
  for insert to authenticated
  with check (
    group_id is not null
    and (is_group_owner(group_id) or is_group_member(group_id))
  );

create policy "categories_update" on categories
  for update to authenticated
  using (group_id is not null and (is_group_owner(group_id) or is_group_member(group_id)))
  with check (group_id is not null and (is_group_owner(group_id) or is_group_member(group_id)));

create policy "categories_delete" on categories
  for delete to authenticated
  using (
    group_id is not null
    and (is_group_owner(group_id) or is_group_member(group_id))
  );

-- ─────────────────────────────────────────────────────────────────────────────
-- 5. spends — reemplazar con helpers
-- ─────────────────────────────────────────────────────────────────────────────
drop policy if exists "spends_select" on spends;
drop policy if exists "spends_insert" on spends;
drop policy if exists "spends_update" on spends;
drop policy if exists "spends_delete" on spends;

create policy "spends_select" on spends
  for select to authenticated
  using (is_group_owner(group_id) or is_group_member(group_id));

create policy "spends_insert" on spends
  for insert to authenticated
  with check (is_group_owner(group_id) or is_group_member(group_id));

create policy "spends_update" on spends
  for update to authenticated
  using (is_group_owner(group_id) or is_group_member(group_id))
  with check (is_group_owner(group_id) or is_group_member(group_id));

create policy "spends_delete" on spends
  for delete to authenticated
  using (is_group_owner(group_id) or is_group_member(group_id));

-- ─────────────────────────────────────────────────────────────────────────────
-- 6. spend_shares — reemplazar (acceso vía spends → group_id)
-- ─────────────────────────────────────────────────────────────────────────────
drop policy if exists "spend_shares_select" on spend_shares;
drop policy if exists "spend_shares_insert" on spend_shares;
drop policy if exists "spend_shares_update" on spend_shares;
drop policy if exists "spend_shares_delete" on spend_shares;

create policy "spend_shares_select" on spend_shares
  for select to authenticated
  using (
    exists (
      select 1 from spends s
      where s.id = spend_shares.spend_id
        and (is_group_owner(s.group_id) or is_group_member(s.group_id))
    )
  );

create policy "spend_shares_insert" on spend_shares
  for insert to authenticated
  with check (
    exists (
      select 1 from spends s
      where s.id = spend_shares.spend_id
        and (is_group_owner(s.group_id) or is_group_member(s.group_id))
    )
  );

create policy "spend_shares_update" on spend_shares
  for update to authenticated
  using (
    exists (
      select 1 from spends s
      where s.id = spend_shares.spend_id
        and (is_group_owner(s.group_id) or is_group_member(s.group_id))
    )
  )
  with check (
    exists (
      select 1 from spends s
      where s.id = spend_shares.spend_id
        and (is_group_owner(s.group_id) or is_group_member(s.group_id))
    )
  );

create policy "spend_shares_delete" on spend_shares
  for delete to authenticated
  using (
    exists (
      select 1 from spends s
      where s.id = spend_shares.spend_id
        and (is_group_owner(s.group_id) or is_group_member(s.group_id))
    )
  );

-- ─────────────────────────────────────────────────────────────────────────────
-- 7. settlements — reemplazar con helpers
-- ─────────────────────────────────────────────────────────────────────────────
drop policy if exists "settlements_select" on settlements;
drop policy if exists "settlements_insert" on settlements;
drop policy if exists "settlements_update" on settlements;
drop policy if exists "settlements_delete" on settlements;

create policy "settlements_select" on settlements
  for select to authenticated
  using (is_group_owner(group_id) or is_group_member(group_id));

create policy "settlements_insert" on settlements
  for insert to authenticated
  with check (is_group_owner(group_id) or is_group_member(group_id));

create policy "settlements_update" on settlements
  for update to authenticated
  using (is_group_owner(group_id) or is_group_member(group_id))
  with check (is_group_owner(group_id) or is_group_member(group_id));

create policy "settlements_delete" on settlements
  for delete to authenticated
  using (is_group_owner(group_id) or is_group_member(group_id));

-- ─────────────────────────────────────────────────────────────────────────────
-- 8. group_invite_tokens — reemplazar con helpers
-- ─────────────────────────────────────────────────────────────────────────────
drop policy if exists "invite_tokens_auth_select" on group_invite_tokens;
drop policy if exists "invite_tokens_insert"      on group_invite_tokens;
drop policy if exists "invite_tokens_delete"      on group_invite_tokens;
-- La policy anon_select NO se toca (no tiene ciclo)

create policy "invite_tokens_auth_select" on group_invite_tokens
  for select to authenticated
  using (
    created_by_user_id = (select auth.uid())
    or is_group_owner(group_id)
    or is_group_member(group_id)
  );

create policy "invite_tokens_insert" on group_invite_tokens
  for insert to authenticated
  with check (
    created_by_user_id = (select auth.uid())
    and is_group_owner(group_id)
  );

create policy "invite_tokens_delete" on group_invite_tokens
  for delete to authenticated
  using (
    created_by_user_id = (select auth.uid())
    or is_group_owner(group_id)
  );

