alter table transfer
    drop column if exists comments
;

alter table transfer_audit
    drop column if exists comments
;