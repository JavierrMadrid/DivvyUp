-- V012__add_recurrence_and_receipt_url.sql
-- Añade recurrencia y URL de recibo a la tabla spends

alter table spends
    add column if not exists recurrence  text not null default 'NONE',
    add column if not exists receipt_url text;

-- Índice útil para consultar gastos recurrentes por grupo
create index if not exists spends_recurrence_idx on spends (group_id, recurrence)
    where recurrence <> 'NONE';

comment on column spends.recurrence  is 'Frecuencia de repetición: NONE | DAILY | WEEKLY | MONTHLY';
comment on column spends.receipt_url is 'URL opcional a la imagen del recibo (ej. Supabase Storage)';

