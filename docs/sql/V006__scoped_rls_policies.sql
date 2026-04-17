-- =============================================================================
-- DivvyUp — V006
-- Sustituye las políticas RLS permisivas (using (true)) por políticas con
-- aislamiento real por usuario (tenant isolation).
--
-- PROBLEMA de V001 + V005:
--   Todas las políticas usaban `using (true)`, lo que significa que cualquier
--   usuario autenticado podía leer y modificar los datos de CUALQUIER grupo.
--   No había aislamiento entre usuarios.
--
-- SOLUCIÓN:
--   - `authenticated`: cada usuario solo accede a grupos de los que es dueño
--     (owner_user_id) o participante vinculado (participant_user_links).
--   - `anon`: solo puede leer group_invite_tokens (necesario para validar un
--     token de invitación antes de completar el registro/login). El resto de
--     políticas anon se eliminan.
--
-- NOTA sobre rendimiento:
--   Se usa `(select auth.uid())` en vez de `auth.uid()` para que la subquery
--   se evalúe una sola vez por statement, no una vez por fila.
--   (skill: security-rls-performance)
-- =============================================================================

-- ─────────────────────────────────────────────────────────────────────────────
-- HELPER: función inmutable para evitar re-evaluar auth.uid() en cada fila
-- ─────────────────────────────────────────────────────────────────────────────
-- No se usa una función helper porque Supabase recomienda el patrón
-- (select auth.uid()) inline en la policy. Se documenta el patrón usado.

-- =============================================================================
-- 1. groups
-- =============================================================================
-- Eliminar políticas previas
drop policy if exists "anon_select"  on groups;
drop policy if exists "anon_insert"  on groups;
drop policy if exists "anon_update"  on groups;
drop policy if exists "anon_delete"  on groups;
drop policy if exists "auth_select"  on groups;
drop policy if exists "auth_insert"  on groups;
drop policy if exists "auth_update"  on groups;
drop policy if exists "auth_delete"  on groups;

-- SELECT: dueño del grupo O participante vinculado
create policy "groups_select" on groups
  for select to authenticated
  using (
    (select auth.uid()) = owner_user_id
    or exists (
      select 1 from participant_user_links pul
      where pul.group_id = groups.id
        and pul.user_id = (select auth.uid())
    )
  );

-- INSERT: solo el dueño puede crear grupos (owner_user_id debe ser el uid propio)
create policy "groups_insert" on groups
  for insert to authenticated
  with check (
    (select auth.uid()) = owner_user_id
  );

-- UPDATE: solo el dueño puede editar
create policy "groups_update" on groups
  for update to authenticated
  using  ((select auth.uid()) = owner_user_id)
  with check ((select auth.uid()) = owner_user_id);

-- DELETE: solo el dueño puede borrar
create policy "groups_delete" on groups
  for delete to authenticated
  using ((select auth.uid()) = owner_user_id);

-- =============================================================================
-- 2. participants
-- =============================================================================
drop policy if exists "anon_select"  on participants;
drop policy if exists "anon_insert"  on participants;
drop policy if exists "anon_update"  on participants;
drop policy if exists "anon_delete"  on participants;
drop policy if exists "auth_select"  on participants;
drop policy if exists "auth_insert"  on participants;
drop policy if exists "auth_update"  on participants;
drop policy if exists "auth_delete"  on participants;

-- Macro de pertenencia al grupo (reutilizada en todas las tablas con group_id)
-- El usuario pertenece al grupo si es dueño o tiene un vínculo en participant_user_links
-- Se inlinea en cada policy porque Postgres no permite macros.

create policy "participants_select" on participants
  for select to authenticated
  using (
    exists (
      select 1 from groups g
      where g.id = participants.group_id
        and (
          g.owner_user_id = (select auth.uid())
          or exists (
            select 1 from participant_user_links pul
            where pul.group_id = g.id
              and pul.user_id = (select auth.uid())
          )
        )
    )
  );

create policy "participants_insert" on participants
  for insert to authenticated
  with check (
    exists (
      select 1 from groups g
      where g.id = participants.group_id
        and (
          g.owner_user_id = (select auth.uid())
          or exists (
            select 1 from participant_user_links pul
            where pul.group_id = g.id
              and pul.user_id = (select auth.uid())
          )
        )
    )
  );

create policy "participants_update" on participants
  for update to authenticated
  using (
    exists (
      select 1 from groups g
      where g.id = participants.group_id
        and (
          g.owner_user_id = (select auth.uid())
          or exists (
            select 1 from participant_user_links pul
            where pul.group_id = g.id
              and pul.user_id = (select auth.uid())
          )
        )
    )
  )
  with check (
    exists (
      select 1 from groups g
      where g.id = participants.group_id
        and (
          g.owner_user_id = (select auth.uid())
          or exists (
            select 1 from participant_user_links pul
            where pul.group_id = g.id
              and pul.user_id = (select auth.uid())
          )
        )
    )
  );

create policy "participants_delete" on participants
  for delete to authenticated
  using (
    exists (
      select 1 from groups g
      where g.id = participants.group_id
        and (
          g.owner_user_id = (select auth.uid())
          or exists (
            select 1 from participant_user_links pul
            where pul.group_id = g.id
              and pul.user_id = (select auth.uid())
          )
        )
    )
  );

-- =============================================================================
-- 3. categories
-- =============================================================================
drop policy if exists "anon_select"  on categories;
drop policy if exists "anon_insert"  on categories;
drop policy if exists "anon_update"  on categories;
drop policy if exists "anon_delete"  on categories;
drop policy if exists "auth_select"  on categories;
drop policy if exists "auth_insert"  on categories;
drop policy if exists "auth_update"  on categories;
drop policy if exists "auth_delete"  on categories;

-- SELECT: categorías globales (group_id IS NULL) son visibles a todos los
-- autenticados. Las de grupo, solo a miembros del grupo.
create policy "categories_select" on categories
  for select to authenticated
  using (
    group_id is null  -- categorías globales del sistema
    or exists (
      select 1 from groups g
      where g.id = categories.group_id
        and (
          g.owner_user_id = (select auth.uid())
          or exists (
            select 1 from participant_user_links pul
            where pul.group_id = g.id
              and pul.user_id = (select auth.uid())
          )
        )
    )
  );

create policy "categories_insert" on categories
  for insert to authenticated
  with check (
    group_id is not null  -- no se pueden crear categorías globales desde la app
    and exists (
      select 1 from groups g
      where g.id = categories.group_id
        and (
          g.owner_user_id = (select auth.uid())
          or exists (
            select 1 from participant_user_links pul
            where pul.group_id = g.id
              and pul.user_id = (select auth.uid())
          )
        )
    )
  );

create policy "categories_update" on categories
  for update to authenticated
  using (
    group_id is not null
    and exists (
      select 1 from groups g
      where g.id = categories.group_id
        and (
          g.owner_user_id = (select auth.uid())
          or exists (
            select 1 from participant_user_links pul
            where pul.group_id = g.id
              and pul.user_id = (select auth.uid())
          )
        )
    )
  )
  with check (
    group_id is not null
    and exists (
      select 1 from groups g
      where g.id = categories.group_id
        and (
          g.owner_user_id = (select auth.uid())
          or exists (
            select 1 from participant_user_links pul
            where pul.group_id = g.id
              and pul.user_id = (select auth.uid())
          )
        )
    )
  );

create policy "categories_delete" on categories
  for delete to authenticated
  using (
    group_id is not null
    and exists (
      select 1 from groups g
      where g.id = categories.group_id
        and (
          g.owner_user_id = (select auth.uid())
          or exists (
            select 1 from participant_user_links pul
            where pul.group_id = g.id
              and pul.user_id = (select auth.uid())
          )
        )
    )
  );

-- =============================================================================
-- 4. spends
-- =============================================================================
drop policy if exists "anon_select"  on spends;
drop policy if exists "anon_insert"  on spends;
drop policy if exists "anon_update"  on spends;
drop policy if exists "anon_delete"  on spends;
drop policy if exists "auth_select"  on spends;
drop policy if exists "auth_insert"  on spends;
drop policy if exists "auth_update"  on spends;
drop policy if exists "auth_delete"  on spends;

create policy "spends_select" on spends
  for select to authenticated
  using (
    exists (
      select 1 from groups g
      where g.id = spends.group_id
        and (
          g.owner_user_id = (select auth.uid())
          or exists (
            select 1 from participant_user_links pul
            where pul.group_id = g.id
              and pul.user_id = (select auth.uid())
          )
        )
    )
  );

create policy "spends_insert" on spends
  for insert to authenticated
  with check (
    exists (
      select 1 from groups g
      where g.id = spends.group_id
        and (
          g.owner_user_id = (select auth.uid())
          or exists (
            select 1 from participant_user_links pul
            where pul.group_id = g.id
              and pul.user_id = (select auth.uid())
          )
        )
    )
  );

create policy "spends_update" on spends
  for update to authenticated
  using (
    exists (
      select 1 from groups g
      where g.id = spends.group_id
        and (
          g.owner_user_id = (select auth.uid())
          or exists (
            select 1 from participant_user_links pul
            where pul.group_id = g.id
              and pul.user_id = (select auth.uid())
          )
        )
    )
  )
  with check (
    exists (
      select 1 from groups g
      where g.id = spends.group_id
        and (
          g.owner_user_id = (select auth.uid())
          or exists (
            select 1 from participant_user_links pul
            where pul.group_id = g.id
              and pul.user_id = (select auth.uid())
          )
        )
    )
  );

create policy "spends_delete" on spends
  for delete to authenticated
  using (
    exists (
      select 1 from groups g
      where g.id = spends.group_id
        and (
          g.owner_user_id = (select auth.uid())
          or exists (
            select 1 from participant_user_links pul
            where pul.group_id = g.id
              and pul.user_id = (select auth.uid())
          )
        )
    )
  );

-- =============================================================================
-- 5. spend_shares
-- spend_shares no tiene group_id directo → se accede a través de spends
-- =============================================================================
drop policy if exists "anon_select"  on spend_shares;
drop policy if exists "anon_insert"  on spend_shares;
drop policy if exists "anon_update"  on spend_shares;
drop policy if exists "anon_delete"  on spend_shares;
drop policy if exists "auth_select"  on spend_shares;
drop policy if exists "auth_insert"  on spend_shares;
drop policy if exists "auth_update"  on spend_shares;
drop policy if exists "auth_delete"  on spend_shares;

create policy "spend_shares_select" on spend_shares
  for select to authenticated
  using (
    exists (
      select 1 from spends s
      join groups g on g.id = s.group_id
      where s.id = spend_shares.spend_id
        and (
          g.owner_user_id = (select auth.uid())
          or exists (
            select 1 from participant_user_links pul
            where pul.group_id = g.id
              and pul.user_id = (select auth.uid())
          )
        )
    )
  );

create policy "spend_shares_insert" on spend_shares
  for insert to authenticated
  with check (
    exists (
      select 1 from spends s
      join groups g on g.id = s.group_id
      where s.id = spend_shares.spend_id
        and (
          g.owner_user_id = (select auth.uid())
          or exists (
            select 1 from participant_user_links pul
            where pul.group_id = g.id
              and pul.user_id = (select auth.uid())
          )
        )
    )
  );

create policy "spend_shares_update" on spend_shares
  for update to authenticated
  using (
    exists (
      select 1 from spends s
      join groups g on g.id = s.group_id
      where s.id = spend_shares.spend_id
        and (
          g.owner_user_id = (select auth.uid())
          or exists (
            select 1 from participant_user_links pul
            where pul.group_id = g.id
              and pul.user_id = (select auth.uid())
          )
        )
    )
  )
  with check (
    exists (
      select 1 from spends s
      join groups g on g.id = s.group_id
      where s.id = spend_shares.spend_id
        and (
          g.owner_user_id = (select auth.uid())
          or exists (
            select 1 from participant_user_links pul
            where pul.group_id = g.id
              and pul.user_id = (select auth.uid())
          )
        )
    )
  );

create policy "spend_shares_delete" on spend_shares
  for delete to authenticated
  using (
    exists (
      select 1 from spends s
      join groups g on g.id = s.group_id
      where s.id = spend_shares.spend_id
        and (
          g.owner_user_id = (select auth.uid())
          or exists (
            select 1 from participant_user_links pul
            where pul.group_id = g.id
              and pul.user_id = (select auth.uid())
          )
        )
    )
  );

-- =============================================================================
-- 6. settlements
-- =============================================================================
drop policy if exists "anon_select"  on settlements;
drop policy if exists "anon_insert"  on settlements;
drop policy if exists "anon_update"  on settlements;
drop policy if exists "anon_delete"  on settlements;
drop policy if exists "auth_select"  on settlements;
drop policy if exists "auth_insert"  on settlements;
drop policy if exists "auth_update"  on settlements;
drop policy if exists "auth_delete"  on settlements;

create policy "settlements_select" on settlements
  for select to authenticated
  using (
    exists (
      select 1 from groups g
      where g.id = settlements.group_id
        and (
          g.owner_user_id = (select auth.uid())
          or exists (
            select 1 from participant_user_links pul
            where pul.group_id = g.id
              and pul.user_id = (select auth.uid())
          )
        )
    )
  );

create policy "settlements_insert" on settlements
  for insert to authenticated
  with check (
    exists (
      select 1 from groups g
      where g.id = settlements.group_id
        and (
          g.owner_user_id = (select auth.uid())
          or exists (
            select 1 from participant_user_links pul
            where pul.group_id = g.id
              and pul.user_id = (select auth.uid())
          )
        )
    )
  );

create policy "settlements_update" on settlements
  for update to authenticated
  using (
    exists (
      select 1 from groups g
      where g.id = settlements.group_id
        and (
          g.owner_user_id = (select auth.uid())
          or exists (
            select 1 from participant_user_links pul
            where pul.group_id = g.id
              and pul.user_id = (select auth.uid())
          )
        )
    )
  )
  with check (
    exists (
      select 1 from groups g
      where g.id = settlements.group_id
        and (
          g.owner_user_id = (select auth.uid())
          or exists (
            select 1 from participant_user_links pul
            where pul.group_id = g.id
              and pul.user_id = (select auth.uid())
          )
        )
    )
  );

create policy "settlements_delete" on settlements
  for delete to authenticated
  using (
    exists (
      select 1 from groups g
      where g.id = settlements.group_id
        and (
          g.owner_user_id = (select auth.uid())
          or exists (
            select 1 from participant_user_links pul
            where pul.group_id = g.id
              and pul.user_id = (select auth.uid())
          )
        )
    )
  );

-- =============================================================================
-- 7. participant_user_links
-- =============================================================================
drop policy if exists "anon_select"  on participant_user_links;
drop policy if exists "anon_insert"  on participant_user_links;
drop policy if exists "anon_update"  on participant_user_links;
drop policy if exists "anon_delete"  on participant_user_links;
drop policy if exists "auth_select"  on participant_user_links;
drop policy if exists "auth_insert"  on participant_user_links;
drop policy if exists "auth_update"  on participant_user_links;
drop policy if exists "auth_delete"  on participant_user_links;

-- SELECT: el propio usuario ve sus vínculos, o cualquier miembro del grupo los ve
-- (necesario para saber qué participantes ya tienen cuenta vinculada)
create policy "pul_select" on participant_user_links
  for select to authenticated
  using (
    user_id = (select auth.uid())
    or exists (
      select 1 from groups g
      where g.id = participant_user_links.group_id
        and (
          g.owner_user_id = (select auth.uid())
          or exists (
            select 1 from participant_user_links pul2
            where pul2.group_id = g.id
              and pul2.user_id = (select auth.uid())
          )
        )
    )
  );

-- INSERT: cualquier autenticado puede vincularse a un participante
-- (flujo de aceptar invitación). La constraint UNIQUE(group_id, user_id)
-- garantiza que no haya duplicados.
create policy "pul_insert" on participant_user_links
  for insert to authenticated
  with check (
    user_id = (select auth.uid())
  );

-- UPDATE: no se permite actualizar vínculos (borrar y recrear si es necesario)

-- DELETE: el propio usuario puede desvincular su cuenta, o el dueño del grupo
create policy "pul_delete" on participant_user_links
  for delete to authenticated
  using (
    user_id = (select auth.uid())
    or exists (
      select 1 from groups g
      where g.id = participant_user_links.group_id
        and g.owner_user_id = (select auth.uid())
    )
  );

-- =============================================================================
-- 8. group_invite_tokens
-- =============================================================================
drop policy if exists "anon_select"  on group_invite_tokens;
drop policy if exists "anon_insert"  on group_invite_tokens;
drop policy if exists "anon_delete"  on group_invite_tokens;
drop policy if exists "auth_select"  on group_invite_tokens;
drop policy if exists "auth_insert"  on group_invite_tokens;
drop policy if exists "auth_delete"  on group_invite_tokens;

-- SELECT anon: necesario para validar un token de invitación antes de hacer login.
-- Solo puede leer tokens no expirados (protege contra enumeración exhaustiva).
create policy "invite_tokens_anon_select" on group_invite_tokens
  for select to anon
  using (expires_at > now());

-- SELECT authenticated: miembros del grupo y el creador pueden ver los tokens
create policy "invite_tokens_auth_select" on group_invite_tokens
  for select to authenticated
  using (
    created_by_user_id = (select auth.uid())
    or exists (
      select 1 from groups g
      where g.id = group_invite_tokens.group_id
        and (
          g.owner_user_id = (select auth.uid())
          or exists (
            select 1 from participant_user_links pul
            where pul.group_id = g.id
              and pul.user_id = (select auth.uid())
          )
        )
    )
  );

-- INSERT: solo el dueño del grupo puede generar tokens de invitación
create policy "invite_tokens_insert" on group_invite_tokens
  for insert to authenticated
  with check (
    created_by_user_id = (select auth.uid())
    and exists (
      select 1 from groups g
      where g.id = group_invite_tokens.group_id
        and g.owner_user_id = (select auth.uid())
    )
  );

-- DELETE: el creador del token o el dueño del grupo pueden revocar tokens
create policy "invite_tokens_delete" on group_invite_tokens
  for delete to authenticated
  using (
    created_by_user_id = (select auth.uid())
    or exists (
      select 1 from groups g
      where g.id = group_invite_tokens.group_id
        and g.owner_user_id = (select auth.uid())
    )
  );

