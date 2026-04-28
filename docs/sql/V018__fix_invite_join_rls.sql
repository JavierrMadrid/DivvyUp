-- =============================================================================
-- DivvyUp — V018
-- Corrige las políticas RLS para que un usuario autenticado que recibe un
-- enlace de invitación pueda ver el grupo y sus participantes ANTES de unirse.
--
-- PROBLEMA:
--   groups_select    → solo owner o member → usuario invitado (aún no member) → vacío
--   participants_select → ídem → vacío
--   invite_tokens_auth_select → solo creator/owner/member → usuario invitado → vacío
--
-- SOLUCIÓN:
--   1. Función SECURITY DEFINER `group_has_valid_invite(p_group_id)` que comprueba
--      si hay un token de invitación vigente para el grupo, sin pasar por RLS.
--   2. Política adicional en `group_invite_tokens` que permite a cualquier usuario
--      autenticado leer tokens vigentes (UUID → no adivinable).
--   3. Actualizar `groups_select` y `participants_select` para permitir acceso
--      si existe un invite token vigente para ese grupo.
-- =============================================================================

-- ─────────────────────────────────────────────────────────────────────────────
-- HELPER: comprueba si el grupo tiene algún token de invitación vigente
-- ─────────────────────────────────────────────────────────────────────────────
create or replace function group_has_valid_invite(p_group_id bigint)
returns boolean
language sql
security definer
stable
set search_path = public
as $$
  select exists (
    select 1 from group_invite_tokens
    where group_id = p_group_id
      and expires_at > now()
  );
$$;

-- ─────────────────────────────────────────────────────────────────────────────
-- group_invite_tokens: permitir lectura de tokens vigentes a cualquier usuario
-- autenticado (para que findValidToken funcione sin ser owner/member)
-- ─────────────────────────────────────────────────────────────────────────────
drop policy if exists "invite_tokens_token_lookup" on group_invite_tokens;

create policy "invite_tokens_token_lookup" on group_invite_tokens
  for select to authenticated
  using (expires_at > now());

-- ─────────────────────────────────────────────────────────────────────────────
-- groups: añadir acceso si hay invite vigente para el grupo
-- ─────────────────────────────────────────────────────────────────────────────
drop policy if exists "groups_select" on groups;

create policy "groups_select" on groups
  for select to authenticated
  using (
    owner_user_id = (select auth.uid())
    or is_group_member(id)
    or group_has_valid_invite(id)
  );

-- ─────────────────────────────────────────────────────────────────────────────
-- participants: añadir acceso si hay invite vigente para el grupo
-- ─────────────────────────────────────────────────────────────────────────────
drop policy if exists "participants_select" on participants;

create policy "participants_select" on participants
  for select to authenticated
  using (
    is_group_owner(group_id)
    or is_group_member(group_id)
    or group_has_valid_invite(group_id)
  );

