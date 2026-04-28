-- V017__add_category_budget.sql
-- Presupuesto mensual opcional por categoría
alter table categories add column if not exists budget numeric(12,2);

comment on column categories.budget is 'Presupuesto mensual opcional para esta categoría (null = sin límite)';

