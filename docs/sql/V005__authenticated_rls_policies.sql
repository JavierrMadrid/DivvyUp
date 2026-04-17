-- =============================================================================
-- DivvyUp — V005
-- Añade políticas RLS para el rol `authenticated`.
-- Las políticas de V001 solo cubren `anon`. Los usuarios que inician sesión
-- con Google/Email usan el rol `authenticated` y no podían insertar ni leer datos.
-- =============================================================================

-- groups
create policy "auth_select" on groups for select to authenticated using (true);
create policy "auth_insert" on groups for insert to authenticated with check (true);
create policy "auth_update" on groups for update to authenticated using (true) with check (true);
create policy "auth_delete" on groups for delete to authenticated using (true);

-- participants
create policy "auth_select" on participants for select to authenticated using (true);
create policy "auth_insert" on participants for insert to authenticated with check (true);
create policy "auth_update" on participants for update to authenticated using (true) with check (true);
create policy "auth_delete" on participants for delete to authenticated using (true);

-- categories
create policy "auth_select" on categories for select to authenticated using (true);
create policy "auth_insert" on categories for insert to authenticated with check (true);
create policy "auth_update" on categories for update to authenticated using (true) with check (true);
create policy "auth_delete" on categories for delete to authenticated using (true);

-- spends
create policy "auth_select" on spends for select to authenticated using (true);
create policy "auth_insert" on spends for insert to authenticated with check (true);
create policy "auth_update" on spends for update to authenticated using (true) with check (true);
create policy "auth_delete" on spends for delete to authenticated using (true);

-- spend_shares
create policy "auth_select" on spend_shares for select to authenticated using (true);
create policy "auth_insert" on spend_shares for insert to authenticated with check (true);
create policy "auth_update" on spend_shares for update to authenticated using (true) with check (true);
create policy "auth_delete" on spend_shares for delete to authenticated using (true);

-- settlements
create policy "auth_select" on settlements for select to authenticated using (true);
create policy "auth_insert" on settlements for insert to authenticated with check (true);
create policy "auth_update" on settlements for update to authenticated using (true) with check (true);
create policy "auth_delete" on settlements for delete to authenticated using (true);

-- participant_user_links (ya tenía anon, añadimos authenticated)
create policy "auth_select" on participant_user_links for select to authenticated using (true);
create policy "auth_insert" on participant_user_links for insert to authenticated with check (true);
create policy "auth_update" on participant_user_links for update to authenticated using (true) with check (true);
create policy "auth_delete" on participant_user_links for delete to authenticated using (true);

-- group_invite_tokens (ya tenía anon, añadimos authenticated)
create policy "auth_select" on group_invite_tokens for select to authenticated using (true);
create policy "auth_insert" on group_invite_tokens for insert to authenticated with check (true);
create policy "auth_delete" on group_invite_tokens for delete to authenticated using (true);

