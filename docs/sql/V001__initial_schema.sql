-- =============================================================================
-- DivvyUp — Migración inicial de base de datos
-- Supabase (Postgres) — V1
-- Revisado contra supabase-postgres-best-practices skill (enero 2026)
-- =============================================================================

-- =============================================================================
-- 1. TABLA: groups
-- Identifiers: snake_case lowercase (skill: schema-lowercase-identifiers)
-- PK: bigint identity — SQL-standard, secuencial, sin fragmentación de índice
--     (skill: schema-primary-keys)
-- Tipos: text (no varchar), timestamptz (no timestamp) (skill: schema-data-types)
-- =============================================================================
create table groups (
  id          bigint generated always as identity primary key,
  name        text not null,
  description text not null default '',
  currency    text not null default 'EUR',
  created_at  timestamptz not null default now()
);

comment on table groups is 'Grupos de gasto compartido';
comment on column groups.currency is 'Código ISO 4217 de la divisa principal del grupo';

-- =============================================================================
-- 2. TABLA: participants
-- FK group_id indexada explícitamente (skill: schema-foreign-key-indexes)
-- =============================================================================
create table participants (
  id          bigint generated always as identity primary key,
  group_id    bigint not null references groups(id) on delete cascade,
  name        text not null,
  email       text,
  created_at  timestamptz not null default now()
);

-- FK index obligatorio — Postgres NO lo crea automáticamente
-- (skill: schema-foreign-key-indexes — 10-100x faster JOINs y CASCADE)
create index participants_group_id_idx on participants (group_id);

comment on table participants is 'Participantes de cada grupo';

-- =============================================================================
-- 3. TABLA: categories
-- group_id nullable: NULL = categoría predefinida global, NOT NULL = por grupo
-- Índice parcial para categorías de grupo (las globales se consultan por separado)
-- (skill: query-partial-indexes — 5-20x smaller indexes)
-- =============================================================================
create table categories (
  id          bigint generated always as identity primary key,
  group_id    bigint references groups(id) on delete cascade,
  name        text not null,
  icon        text not null default '📦',
  color       text not null default '#6366F1',
  is_default  boolean not null default false,
  created_at  timestamptz not null default now()
);

-- Índice parcial: solo categorías de grupo (group_id NOT NULL)
-- Las categorías globales (group_id IS NULL) son pocas filas, no necesitan índice
-- (skill: query-partial-indexes)
create index categories_group_id_idx on categories (group_id)
  where group_id is not null;

comment on table categories is 'Categorías de gasto (globales si group_id es null, o por grupo)';
comment on column categories.icon is 'Emoji o nombre de icono Material';
comment on column categories.color is 'Color hex para UI (#RRGGBB)';
comment on column categories.is_default is 'true = categoría predefinida del sistema (no editable)';

-- =============================================================================
-- 4. TABLA: spends
-- split_type como text con CHECK constraint (skill: schema-data-types)
-- amount como numeric(12,2) — precisión exacta para dinero (no float)
-- Índice compuesto (group_id, date DESC): cubre queries por grupo Y por rango de fecha.
-- Gracias al leftmost prefix rule, también cubre queries solo por group_id.
-- Se elimina el índice simple group_id_idx que era redundante.
-- (skill: query-composite-indexes — leftmost prefix rule)
-- =============================================================================
create table spends (
  id          bigint generated always as identity primary key,
  group_id    bigint not null references groups(id) on delete cascade,
  concept     text not null,
  amount      numeric(12,2) not null check (amount > 0),
  date        timestamptz not null default now(),
  payer_id    bigint not null references participants(id),
  category_id bigint references categories(id) on delete set null,
  split_type  text not null default 'EQUAL'
                check (split_type in ('EQUAL', 'PERCENTAGE', 'CUSTOM')),
  notes       text not null default '',
  created_at  timestamptz not null default now()
);

-- Índice compuesto (group_id, date DESC): cubre queries por grupo Y por rango de fecha.
-- El leftmost prefix rule lo hace válido también para WHERE group_id = ?
-- (skill: query-composite-indexes)
create index spends_group_id_date_idx on spends (group_id, date desc);

-- FK indexes individuales para payer_id y category_id (JOINs y cascadas)
-- (skill: schema-foreign-key-indexes)
create index spends_payer_id_idx    on spends (payer_id);
create index spends_category_id_idx on spends (category_id);

comment on table spends is 'Gastos registrados en cada grupo';
comment on column spends.amount is 'Importe total del gasto (numeric para precisión monetaria exacta)';
comment on column spends.split_type is 'Tipo de reparto: EQUAL (equitativo), PERCENTAGE (%), CUSTOM (importes fijos)';

-- =============================================================================
-- 5. TABLA: spend_shares
-- UNIQUE(spend_id, participant_id) crea implícitamente un índice compuesto.
-- Solo se añade índice adicional en participant_id para queries inversas.
-- CHECK en percentage para validar rango 0-100 (skill: schema-constraints)
-- (skill: schema-foreign-key-indexes)
-- =============================================================================
create table spend_shares (
  id              bigint generated always as identity primary key,
  spend_id        bigint not null references spends(id) on delete cascade,
  participant_id  bigint not null references participants(id) on delete cascade,
  amount          numeric(12,2) not null check (amount >= 0),
  percentage      numeric(5,2)  check (percentage is null or (percentage >= 0 and percentage <= 100)),
  created_at      timestamptz not null default now(),

  -- Garantiza que un participante no aparezca dos veces en el mismo gasto.
  -- UNIQUE crea implícitamente un índice en (spend_id, participant_id).
  unique (spend_id, participant_id)
);

-- El UNIQUE ya indexa (spend_id, participant_id).
-- Solo añadimos índice en participant_id para queries inversas (ej: gastos de X persona).
-- (skill: schema-foreign-key-indexes)
create index spend_shares_participant_id_idx on spend_shares (participant_id);

comment on table spend_shares is 'Reparto de cada gasto entre los participantes';
comment on column spend_shares.amount is 'Cantidad que le corresponde pagar a este participante';
comment on column spend_shares.percentage is 'Porcentaje asignado (solo si split_type=PERCENTAGE, 0-100)';

-- =============================================================================
-- 6. TABLA: settlements
-- Índice compuesto (group_id, date DESC) — leftmost prefix también cubre group_id solo
-- CHECK: from_participant_id <> to_participant_id
-- (skill: query-composite-indexes)
-- =============================================================================
create table settlements (
  id                    bigint generated always as identity primary key,
  group_id              bigint not null references groups(id) on delete cascade,
  from_participant_id   bigint not null references participants(id),
  to_participant_id     bigint not null references participants(id),
  amount                numeric(12,2) not null check (amount > 0),
  date                  timestamptz not null default now(),
  notes                 text not null default '',
  created_at            timestamptz not null default now(),

  check (from_participant_id <> to_participant_id)
);

-- Índice compuesto (group_id, date DESC) para historial ordenado por grupo
-- (skill: query-composite-indexes — leftmost prefix rule)
create index settlements_group_id_date_idx       on settlements (group_id, date desc);

-- FK indexes para lookups por participante (skill: schema-foreign-key-indexes)
create index settlements_from_participant_id_idx on settlements (from_participant_id);
create index settlements_to_participant_id_idx   on settlements (to_participant_id);

comment on table settlements is 'Liquidaciones/pagos entre participantes para saldar deudas';
comment on column settlements.from_participant_id is 'Participante que paga la deuda';
comment on column settlements.to_participant_id is 'Participante que recibe el pago';

-- =============================================================================
-- 7. SEED: categorías predefinidas del sistema
-- group_id = NULL indica categoría global (disponible en todos los grupos)
-- =============================================================================
insert into categories (group_id, name, icon, color, is_default) values
  (null, 'Comida',          '🍽️', '#EF4444', true),
  (null, 'Transporte',      '🚗', '#3B82F6', true),
  (null, 'Alojamiento',     '🏠', '#8B5CF6', true),
  (null, 'Entretenimiento', '🎬', '#F59E0B', true),
  (null, 'Compras',         '🛒', '#10B981', true),
  (null, 'Supermercado',    '🏪', '#06B6D4', true),
  (null, 'Salud',           '💊', '#EC4899', true),
  (null, 'Servicios',       '💡', '#F97316', true),
  (null, 'Viajes',          '✈️',  '#6366F1', true),
  (null, 'Otros',           '📦', '#6B7280', true);

-- =============================================================================
-- 8. ROW LEVEL SECURITY
-- (skill: security-rls-basics — CRITICAL, database-enforced tenant isolation)
-- (skill: security-rls-performance — usar (select auth.uid()) no auth.uid())
--
-- Políticas desglosadas por operación con "to anon" explícito.
-- Más seguro y granular que "for all" sin rol especificado.
-- Cuando se añada auth, reemplazar por:
--   create policy "users_own_data" on <table>
--     for all to authenticated
--     using ((select auth.uid()) = user_id);
-- Nota: (select auth.uid()) en vez de auth.uid() — se evalúa una vez, no por fila.
-- =============================================================================
alter table groups        enable row level security;
alter table participants  enable row level security;
alter table categories    enable row level security;
alter table spends        enable row level security;
alter table spend_shares  enable row level security;
alter table settlements   enable row level security;

-- groups
create policy "anon_select" on groups for select to anon using (true);
create policy "anon_insert" on groups for insert to anon with check (true);
create policy "anon_update" on groups for update to anon using (true) with check (true);
create policy "anon_delete" on groups for delete to anon using (true);

-- participants
create policy "anon_select" on participants for select to anon using (true);
create policy "anon_insert" on participants for insert to anon with check (true);
create policy "anon_update" on participants for update to anon using (true) with check (true);
create policy "anon_delete" on participants for delete to anon using (true);

-- categories
create policy "anon_select" on categories for select to anon using (true);
create policy "anon_insert" on categories for insert to anon with check (true);
create policy "anon_update" on categories for update to anon using (true) with check (true);
create policy "anon_delete" on categories for delete to anon using (true);

-- spends
create policy "anon_select" on spends for select to anon using (true);
create policy "anon_insert" on spends for insert to anon with check (true);
create policy "anon_update" on spends for update to anon using (true) with check (true);
create policy "anon_delete" on spends for delete to anon using (true);

-- spend_shares
create policy "anon_select" on spend_shares for select to anon using (true);
create policy "anon_insert" on spend_shares for insert to anon with check (true);
create policy "anon_update" on spend_shares for update to anon using (true) with check (true);
create policy "anon_delete" on spend_shares for delete to anon using (true);

-- settlements
create policy "anon_select" on settlements for select to anon using (true);
create policy "anon_insert" on settlements for insert to anon with check (true);
create policy "anon_update" on settlements for update to anon using (true) with check (true);
create policy "anon_delete" on settlements for delete to anon using (true);

-- =============================================================================
-- 9. VISTAS con security_invoker = on
--
-- CRÍTICO para Supabase: por defecto las vistas usan SECURITY DEFINER
-- (ejecutan con privilegios del owner, bypasseando RLS completamente).
-- Con security_invoker = on, la vista respeta el RLS del usuario llamante.
-- (skill: security-rls-basics — Supabase specific)
-- =============================================================================

-- ---------------------------------------------------------------------------
-- Vista: Balance neto de cada participante en su grupo
--
-- CORRECCIÓN respecto a la versión original:
--   BUG: la subconsulta de total_owed usaba una subquery correlacionada incorrecta:
--     where spends.group_id = (select group_id from participants where id = spend_shares.participant_id)
--   Esto es ambiguo y potencialmente incorrecto cuando hay participantes en varios grupos.
--   FIX: JOIN directo entre spend_shares, spends y participants filtrando por p.group_id.
--
-- net_balance > 0 → le deben dinero
-- net_balance < 0 → debe dinero
-- ---------------------------------------------------------------------------
create or replace view participant_balances
with (security_invoker = on)
as
select
  p.id                                                as participant_id,
  p.group_id,
  p.name                                              as participant_name,

  -- Total pagado por este participante como pagador de gastos de su grupo
  coalesce(paid.total_paid, 0)                        as total_paid,

  -- Total que le corresponde pagar (sus spend_shares en gastos de su grupo)
  coalesce(owed.total_owed, 0)                        as total_owed,

  -- Liquidaciones que ha recibido (alguien le pagó)
  coalesce(recv.total_received, 0)                    as settlements_received,

  -- Liquidaciones que ha enviado (él pagó a alguien)
  coalesce(sent.total_sent, 0)                        as settlements_sent,

  -- Balance neto = pagado - corresponde + recibido - enviado
  (
    coalesce(paid.total_paid, 0)
    - coalesce(owed.total_owed, 0)
    + coalesce(recv.total_received, 0)
    - coalesce(sent.total_sent, 0)
  )                                                   as net_balance

from participants p

-- Lo que pagó como pagador principal en gastos de su grupo
left join (
  select payer_id, sum(amount) as total_paid
  from spends
  group by payer_id
) paid on paid.payer_id = p.id

-- Lo que le corresponde pagar según spend_shares, filtrando por su grupo
-- FIX: JOIN directo — correcto y eficiente, usa el índice spend_shares_participant_id_idx
left join (
  select ss.participant_id, sum(ss.amount) as total_owed
  from spend_shares ss
  join spends s on s.id = ss.spend_id
  join participants p_inner on p_inner.id = ss.participant_id
  where s.group_id = p_inner.group_id
  group by ss.participant_id
) owed on owed.participant_id = p.id

-- Liquidaciones recibidas
left join (
  select to_participant_id, sum(amount) as total_received
  from settlements
  group by to_participant_id
) recv on recv.to_participant_id = p.id

-- Liquidaciones enviadas
left join (
  select from_participant_id, sum(amount) as total_sent
  from settlements
  group by from_participant_id
) sent on sent.from_participant_id = p.id;

comment on view participant_balances
  is 'Balance neto por participante. security_invoker=on respeta RLS. net_balance>0 = le deben; <0 = debe.';

-- ---------------------------------------------------------------------------
-- Vista: Resumen de gastos por categoría (para pie chart)
-- ---------------------------------------------------------------------------
create or replace view spend_summary_by_category
with (security_invoker = on)
as
select
  s.group_id,
  s.category_id,
  coalesce(c.name,  'Sin categoría') as category_name,
  coalesce(c.icon,  '📦')            as category_icon,
  coalesce(c.color, '#6B7280')       as category_color,
  count(*)::bigint                   as spend_count,
  sum(s.amount)                      as total_amount,
  avg(s.amount)                      as avg_amount,
  min(s.date)                        as first_spend_date,
  max(s.date)                        as last_spend_date
from spends s
left join categories c on c.id = s.category_id
group by s.group_id, s.category_id, c.name, c.icon, c.color;

comment on view spend_summary_by_category
  is 'Resumen por categoría y grupo (pie chart). security_invoker=on.';

-- ---------------------------------------------------------------------------
-- Vista: Resumen de gastos por mes (para bar chart)
-- NOTA: ORDER BY eliminado — las vistas no garantizan orden. Usar ORDER BY month
--       en la query que consuma esta vista.
-- date_trunc con 'UTC' explícito para consistencia entre timezones.
-- ---------------------------------------------------------------------------
create or replace view spend_summary_by_month
with (security_invoker = on)
as
select
  s.group_id,
  date_trunc('month', s.date at time zone 'UTC')::date as month,
  count(*)::bigint                                      as spend_count,
  sum(s.amount)                                         as total_amount,
  avg(s.amount)                                         as avg_amount
from spends s
group by s.group_id, date_trunc('month', s.date at time zone 'UTC');

comment on view spend_summary_by_month
  is 'Resumen por mes UTC y grupo (bar chart). security_invoker=on. Ordenar con ORDER BY month.';

-- =============================================================================
-- 10. DIAGNÓSTICO: query para detectar FKs sin índice
-- Ejecutar en Supabase SQL Editor para verificar que no falten índices en FKs.
-- (skill: schema-foreign-key-indexes)
-- =============================================================================
-- select
--   conrelid::regclass as table_name,
--   a.attname          as fk_column
-- from pg_constraint c
-- join pg_attribute a on a.attrelid = c.conrelid and a.attnum = any(c.conkey)
-- where c.contype = 'f'
--   and not exists (
--     select 1 from pg_index i
--     where i.indrelid = c.conrelid and a.attnum = any(i.indkey)
--   );
