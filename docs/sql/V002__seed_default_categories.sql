-- =============================================================================
-- DivvyUp — Seed: categorías predefinidas globales (group_id IS NULL)
-- Ejecutar en Supabase SQL Editor
-- is_default = true → no editables por el usuario
-- =============================================================================

insert into categories (group_id, name, icon, color, is_default) values
  (null, 'Comida',       '🍽️',  '#FF6B6B', true),
  (null, 'Transporte',   '🚗',  '#4FC3F7', true),
  (null, 'Alojamiento',  '🏠',  '#9B59B6', true),
  (null, 'Ocio',         '🎉',  '#FFD93D', true),
  (null, 'Compras',      '🛍️',  '#00C9A7', true),
  (null, 'Salud',        '💊',  '#FF8A65', true),
  (null, 'Deportes',     '⚽',  '#26A69A', true),
  (null, 'Viajes',       '✈️',  '#5C6BC0', true),
  (null, 'Mascotas',     '🐾',  '#8D6E63', true),
  (null, 'Otros',        '📦',  '#6366F1', true)
on conflict do nothing;

