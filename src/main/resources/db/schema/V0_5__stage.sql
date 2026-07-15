create type transfer_stage as enum ('PLANNING', 'SCHEDULED')
;

alter table transfer
    add column stage transfer_stage
;

alter table transfer_audit
    add column stage transfer_stage
;

update transfer t
set stage = case when (select ts.code from transfer_status ts where ts.id = t.status_id) = 'SCHEDULED' then 'SCHEDULED'::transfer_stage else 'PLANNING'::transfer_stage end
where t.stage is null
;

update transfer_audit ta
set stage = case when (select ts.code from transfer_status ts where ts.id = ta.status_id) = 'SCHEDULED' then 'SCHEDULED'::transfer_stage else 'PLANNING'::transfer_stage end
where ta.stage is null
;

alter table transfer
    alter column stage set not null
;

alter table transfer_audit
    alter column stage set not null
;