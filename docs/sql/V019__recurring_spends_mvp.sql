-- V019__recurring_spends_mvp.sql
-- Activa los gastos recurrentes automáticos:
--   · recurrence_parent_id → FK al gasto raíz del que deriva esta ocurrencia
--   · recurrence_next_due  → cuándo debe generarse la siguiente ocurrencia (solo en el gasto raíz)
-- Las ocurrencias son gastos normales; su recurrence = 'NONE' y recurrence_parent_id = root.id

alter table spends
    add column if not exists recurrence_parent_id bigint references spends(id) on delete cascade,
    add column if not exists recurrence_next_due  timestamptz;

-- Índice para localizar ocurrencias por su gasto raíz (FK no se indexa automáticamente)
create index if not exists spends_recurrence_parent_id_idx
    on spends (recurrence_parent_id)
    where recurrence_parent_id is not null;

-- Índice para encontrar eficientemente todos los gastos raíz vencidos de un grupo
create index if not exists spends_recurrence_due_idx
    on spends (group_id, recurrence_next_due)
    where recurrence <> 'NONE' and recurrence_parent_id is null;

-- Restricción única: una sola ocurrencia por (raíz, fecha programada)
-- Evita duplicados si loadAll() se llama varias veces antes de que el servidor responda
-- ADD CONSTRAINT no soporta IF NOT EXISTS en Postgres → usamos DO block
do $$
begin
    if not exists (
        select 1 from pg_constraint
        where conname = 'spends_recurrence_unique_occurrence'
    ) then
        alter table spends
            add constraint spends_recurrence_unique_occurrence
            unique (recurrence_parent_id, recurrence_next_due);
    end if;
end $$;

comment on column spends.recurrence_parent_id is
    'FK al gasto raíz del que este gasto es una ocurrencia generada automáticamente. NULL en gastos raíz y gastos puntuales.';
comment on column spends.recurrence_next_due is
    'Solo en gastos raíz (recurrence <> NONE): fecha en que debe generarse la siguiente ocurrencia.';

