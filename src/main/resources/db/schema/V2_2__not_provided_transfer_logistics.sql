insert
into transfer_logistics(code, description, sequence_number, active)
select 'NOT_PROVIDED', 'Not provided', 990, false
on conflict do nothing;