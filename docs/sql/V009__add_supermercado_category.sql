-- =============================================================================
-- DivvyUp — V009: Añadir categoría global "Supermercado"
-- =============================================================================

insert into categories (group_id, name, icon, color, is_default)
values (null, 'Supermercado', '🛒', '#43A047', true)
on conflict do nothing;

