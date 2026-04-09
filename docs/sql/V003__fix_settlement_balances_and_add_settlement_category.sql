-- =============================================================================
-- DivvyUp — V003
-- 1) Corrige el signo de las liquidaciones en la vista participant_balances
-- 2) Añade la categoría global por defecto "Liquidación"
-- =============================================================================

insert into categories (group_id, name, icon, color, is_default)
select null, 'Liquidación', '💸', '#14B8A6', true
where not exists (
  select 1
  from categories
  where group_id is null
    and lower(name) = lower('Liquidación')
);

create or replace view participant_balances
with (security_invoker = on)
as
select
  p.id                                                as participant_id,
  p.group_id,
  p.name                                              as participant_name,
  coalesce(paid.total_paid, 0)                        as total_paid,
  coalesce(owed.total_owed, 0)                        as total_owed,
  coalesce(recv.total_received, 0)                    as settlements_received,
  coalesce(sent.total_sent, 0)                        as settlements_sent,
  (
    coalesce(paid.total_paid, 0)
    - coalesce(owed.total_owed, 0)
    - coalesce(recv.total_received, 0)
    + coalesce(sent.total_sent, 0)
  )                                                   as net_balance
from participants p
left join (
  select payer_id, sum(amount) as total_paid
  from spends
  group by payer_id
) paid on paid.payer_id = p.id
left join (
  select ss.participant_id, sum(ss.amount) as total_owed
  from spend_shares ss
  join spends s on s.id = ss.spend_id
  join participants p_inner on p_inner.id = ss.participant_id
  where s.group_id = p_inner.group_id
  group by ss.participant_id
) owed on owed.participant_id = p.id
left join (
  select to_participant_id, sum(amount) as total_received
  from settlements
  group by to_participant_id
) recv on recv.to_participant_id = p.id
left join (
  select from_participant_id, sum(amount) as total_sent
  from settlements
  group by from_participant_id
) sent on sent.from_participant_id = p.id;

comment on view participant_balances
  is 'Balance neto por participante. net_balance>0 = le deben; <0 = debe. Las liquidaciones reducen saldo del receptor y aumentan el del pagador.';

