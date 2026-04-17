-- =============================================================================
-- DivvyUp — V008
-- PROBLEMA:
--   V006 eliminó todas las políticas `anon` de la tabla `categories`.
--   El cliente Supabase usa el rol `anon` cuando no hay sesión iniciada
--   (y también en algunas llamadas internas), lo que hace que las categorías
--   globales (group_id IS NULL) sean invisibles aunque existan en la BD.
--
-- SOLUCIÓN:
--   1. Re-añadir política SELECT para `anon` restringida a categorías globales
--      (group_id IS NULL). Las categorías de grupo siguen siendo privadas.
--   2. Re-insertar las categorías por defecto por si se perdieron accidentalmente.
-- =============================================================================

-- 1. Política anon SELECT: solo categorías globales del sistema
drop policy if exists "categories_anon_select" on categories;

create policy "categories_anon_select" on categories
  for select to anon
  using (group_id is null);

-- 2. Re-seed de categorías globales por defecto (idempotente con ON CONFLICT DO NOTHING)
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
  (null, 'Otros',        '📦',  '#6366F1', true),
  (null, 'Liquidación',  '🤝',  '#14B8A6', true)
on conflict do nothing;

