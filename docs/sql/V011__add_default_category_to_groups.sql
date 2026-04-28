-- V011: Añade columna default_category_id a groups para permitir
--       preseleccionar una categoría por defecto al crear gastos en el grupo.

alter table groups
    add column default_category_id bigint references categories(id) on delete set null;

-- Índice para acelerar consultas por categoría por defecto (opcional pero recomendado)
create index groups_default_category_id_idx on groups (default_category_id);

