-- V020__spends_pagination_index.sql
-- Índice compuesto para paginación keyset de gastos por (group_id, date DESC, id DESC).
-- La lista de gastos se pagina por cursor: (date < X) OR (date = X AND id < Y),
-- por lo que el índice incluye el desempate por id para que la búsqueda sea por
-- índice (sin scans profundos con OFFSET) aunque haya miles/millones de gastos.

create index if not exists spends_group_date_id_idx
    on spends (group_id, date desc, id desc);