# DivvyUp — Modelo de Base de Datos

## Diagrama Entidad-Relación

```
┌─────────────────────┐
│       groups         │
├─────────────────────┤
│ id          PK bigint│ ◄──────────────────────────────────────┐
│ name        text     │                                        │
│ description text     │                                        │
│ currency    text     │                                        │
│ created_at  tstz     │                                        │
└─────────┬───────────┘                                        │
          │ 1:N                                                 │
          ▼                                                     │
┌─────────────────────┐      ┌─────────────────────┐           │
│   participants       │      │    categories        │           │
├─────────────────────┤      ├─────────────────────┤           │
│ id          PK bigint│ ◄─┐ │ id          PK bigint│ ◄─┐      │
│ group_id    FK bigint│   │ │ group_id    FK bigint│───┘      │
│ name        text     │   │ │ name        text     │   (null=global)
│ email       text?    │   │ │ icon        text     │           │
│ created_at  tstz     │   │ │ color       text     │           │
└─────────────────────┘   │ │ is_default  boolean  │           │
          ▲                │ │ created_at  tstz     │           │
          │                │ └─────────────────────┘           │
          │                │          ▲                          │
          │                │          │                          │
          │                │          │ 0..1                     │
          │                │ ┌────────┴────────────┐           │
          │                │ │      spends          │           │
          │                │ ├─────────────────────┤           │
          │                │ │ id          PK bigint│ ◄─┐      │
          │                │ │ group_id    FK bigint│───┘──────┘
          │                └─│ payer_id    FK bigint│
          │                  │ category_id FK bigint│
          │                  │ concept     text     │
          │                  │ amount      numeric  │
          │                  │ date        tstz     │
          │                  │ split_type  text     │
          │                  │ notes       text     │
          │                  │ created_at  tstz     │
          │                  └────────┬────────────┘
          │                           │ 1:N
          │                           ▼
          │                  ┌─────────────────────┐
          │                  │   spend_shares       │
          │                  ├─────────────────────┤
          │                  │ id          PK bigint│
          ├──────────────────│ participant_id FK    │
          │                  │ spend_id       FK    │
          │                  │ amount      numeric  │
          │                  │ percentage  numeric? │
          │                  │ created_at  tstz     │
          │                  │ UNIQUE(spend,partic.)│
          │                  └─────────────────────┘
          │
          │                  ┌─────────────────────┐
          │                  │   settlements        │
          │                  ├─────────────────────┤
          │                  │ id          PK bigint│
          │                  │ group_id    FK bigint│
          ├──────────────────│ from_participant_id  │
          └──────────────────│ to_participant_id    │
                             │ amount      numeric  │
                             │ date        tstz     │
                             │ notes       text     │
                             │ created_at  tstz     │
                             │ CHECK(from ≠ to)     │
                             └─────────────────────┘
```

## Tablas y Propósito

| Tabla | Registros típicos | Propósito |
|-------|-------------------|-----------|
| `groups` | 5-50 por usuario | Grupos de gasto compartido |
| `participants` | 2-15 por grupo | Miembros del grupo |
| `categories` | 10 globales + 0-10 por grupo | Clasificación de gastos |
| `spends` | 10-500 por grupo | Gastos registrados |
| `spend_shares` | N × participantes por gasto | Reparto detallado de cada gasto |
| `settlements` | 0-50 por grupo | Liquidaciones entre participantes |
| `participant_user_links` | 1 por usuario y grupo | Vínculo entre cuenta autenticada y participante |

## Relaciones

| Relación | Tipo | FK | Índice | On Delete |
|----------|------|-----|--------|-----------|
| groups → participants | 1:N | `participants.group_id` | ✅ | CASCADE |
| groups → categories | 1:N | `categories.group_id` | ✅ | CASCADE |
| groups → spends | 1:N | `spends.group_id` | ✅ | CASCADE |
| groups → settlements | 1:N | `settlements.group_id` | ✅ | CASCADE |
| participants → spends (payer) | 1:N | `spends.payer_id` | ✅ | — |
| categories → spends | 1:N | `spends.category_id` | ✅ | SET NULL |
| spends → spend_shares | 1:N | `spend_shares.spend_id` | ✅ | CASCADE |
| participants → spend_shares | 1:N | `spend_shares.participant_id` | ✅ | CASCADE |
| participants → settlements (from) | 1:N | `settlements.from_participant_id` | ✅ | — |
| participants → settlements (to) | 1:N | `settlements.to_participant_id` | ✅ | — |
| groups → participant_user_links | 1:N | `participant_user_links.group_id` | ✅ | CASCADE |
| participants → participant_user_links | 1:1 | `participant_user_links.participant_id` | ✅ (UNIQUE) | CASCADE |

## Índices

Todos los FK tienen índice creado explícitamente. Índices adicionales:

| Índice | Columnas | Propósito |
|--------|----------|-----------|
| `spends_date_idx` | `(group_id, date)` | Filtrado por rango de fechas dentro de un grupo |
| `settlements_date_idx` | `(group_id, date)` | Filtrado de liquidaciones por fecha |
| `participant_user_links_group_id_idx` | `(group_id)` | Búsqueda de vínculos por grupo |
| `participant_user_links_user_id_idx` | `(user_id)` | Resolver participante de un usuario |
| UNIQUE en `spend_shares` | `(spend_id, participant_id)` | Evitar duplicados en reparto |
| UNIQUE en `participant_user_links` | `(group_id, user_id)` y `(participant_id)` | Un usuario/participante por grupo |

## Tipos de Reparto (split_type)

| Valor | Descripción | Cómo se calculan spend_shares |
|-------|-------------|-------------------------------|
| `EQUAL` | Equitativo | `amount / N` para cada participante seleccionado |
| `PERCENTAGE` | Por porcentaje | `amount × percentage / 100` por participante |
| `CUSTOM` | Importe personalizado | El usuario define el amount de cada share |

## Vistas Precalculadas

| Vista | Uso |
|-------|-----|
| `participant_balances` | Balance neto de cada participante (pagado - corresponde + liquidaciones) |
| `spend_summary_by_category` | Resumen de gastos por categoría (para gráfico circular) |
| `spend_summary_by_month` | Resumen de gastos por mes (para gráfico de barras) |

## Decisiones de Diseño

### ¿Por qué `spend_shares` en vez de calcular en la app?
- Permite repartos no equitativos (porcentaje, custom) sin lógica compleja en queries
- La "verdad" del reparto queda persistida, no depende de recalcular
- Facilita filtrar "gastos donde participa X" con un simple JOIN

### ¿Por qué `settlements` separado de `spends`?
- Semánticamente diferentes: un gasto es algo que se compró, una liquidación es una transferencia entre personas
- Permite calcular balances de forma clara: balance = pagado - corresponde + recibido - enviado
- Facilita el historial de pagos entre dos personas concretas

### ¿Por qué `numeric(12,2)` y no `float`?
- `float` tiene errores de redondeo (ej: 0.1 + 0.2 ≠ 0.3)
- `numeric` tiene precisión exacta, esencial para cálculos monetarios
- `(12,2)` permite hasta 9,999,999,999.99

### ¿Por qué `bigint identity` y no UUID?
- Siguiendo las mejores prácticas de Supabase para IDs secuenciales
- UUID v4 random causa fragmentación de índice B-tree en tablas grandes
- Para tablas internas sin exposición pública, bigint es más eficiente

### ¿Por qué RLS habilitado desde el principio?
- Buena práctica de seguridad: nunca dejar tablas sin RLS
- Las políticas de V006 implementan aislamiento real por usuario (tenant isolation)
- Un usuario solo puede ver/modificar grupos de los que es dueño (`owner_user_id`) o participante vinculado (`participant_user_links`)

### Lógica de pertenencia a un grupo (usada en todas las RLS de V006)
```sql
-- El usuario pertenece al grupo si:
g.owner_user_id = (select auth.uid())   -- es el dueño
OR exists (
  select 1 from participant_user_links pul
  where pul.group_id = g.id
    and pul.user_id = (select auth.uid())  -- o es participante vinculado
)
```
`(select auth.uid())` se evalúa una vez por statement (no por fila) para mayor rendimiento.

---

## Revisión contra `supabase-postgres-best-practices` skill

Schema auditado contra la skill oficial `supabase/agent-skills` (enero 2026). Cambios aplicados:

### 🐛 Bug crítico corregido — Vista `participant_balances`

**Problema original:** La subconsulta de `total_owed` usaba una subquery correlacionada incorrecta:
```sql
-- ❌ INCORRECTO — ambiguo y potencialmente erróneo
where spends.group_id = (select group_id from participants where id = spend_shares.participant_id)
```
Cuando un participante con el mismo `id` existe en múltiples grupos, el resultado era incorrecto.

**Fix aplicado:** JOIN directo entre `spend_shares`, `spends` y `participants` filtrando explícitamente por `p_inner.group_id`:
```sql
-- ✅ CORRECTO — join directo, usa índice spend_shares_participant_id_idx
join participants p_inner on p_inner.id = ss.participant_id
where s.group_id = p_inner.group_id
```

### 🔒 RLS — Políticas granulares por operación con rol explícito
**Skill:** `security-rls-basics`

**Antes:** Una única política `for all using (true)` sin rol especificado.
**Ahora:** 4 políticas por tabla (SELECT, INSERT, UPDATE, DELETE) con `to anon` explícito. Más seguro y granular — facilita sustituir cada operación individualmente cuando se añada auth.

**Patrón futuro con auth (skill: `security-rls-performance`):**
```sql
-- (select auth.uid()) en vez de auth.uid() — se evalúa una vez, no por fila
create policy "auth_own_data" on groups
  for all to authenticated
  using ((select auth.uid()) = owner_id);
```
**Implementado en V006:** políticas con aislamiento real por usuario.

### 🔒 Vistas — `security_invoker = on`
**Skill:** `security-rls-basics` (Supabase specific)

Las vistas en Postgres usan por defecto `SECURITY DEFINER` (privilegios del owner), lo que **bypasea RLS completamente**. Todas las vistas ahora usan `with (security_invoker = on)`.

### 📊 Índices — Composites y parciales optimizados
**Skills:** `query-composite-indexes`, `query-partial-indexes`

| Cambio | Motivo |
|--------|--------|
| `spends_group_id_idx` + `spends_date_idx` → `spends_group_id_date_idx (group_id, date desc)` | Un solo índice compuesto cubre ambos casos via leftmost prefix rule |
| `settlements_group_id_idx` + `settlements_date_idx` → `settlements_group_id_date_idx (group_id, date desc)` | Idem |
| `categories_group_id_idx` → índice **parcial** `WHERE group_id IS NOT NULL` | Las categorías globales (IS NULL) son ~10 filas; índice parcial 5-20x más pequeño |
| `spend_shares_spend_id_idx` eliminado | El constraint `UNIQUE(spend_id, participant_id)` ya crea ese índice implícitamente |

### ✅ Constraints mejorados
**Skill:** `schema-constraints`

- `percentage` en `spend_shares`: añadido CHECK `(percentage >= 0 AND percentage <= 100)` para validar rango válido.

### 📝 Vistas — ORDER BY eliminado
Las vistas no garantizan orden. Se eliminó `ORDER BY` de `spend_summary_by_month` — el consumidor de la vista debe especificar `ORDER BY month`.

### 🕐 Fechas — timezone explícito en vistas
`date_trunc('month', s.date at time zone 'UTC')` en vez de `date_trunc('month', s.date)` para consistencia independientemente del timezone del servidor Postgres.

