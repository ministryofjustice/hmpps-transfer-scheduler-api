insert into reference_data_domain(code, description)
values ('TRANSFER_CANCELLATION_REASON', 'Transfer cancellation reason')
on conflict do nothing
;

create table if not exists transfer_cancellation_reason
(
    id              uuid         not null default uuidv7(),
    code            varchar(32)  not null,
    description     varchar(255) not null,
    sequence_number int          not null,
    active          boolean      not null,
    constraint pk_transfer_cancellation_reason primary key (id),
    constraint uq_transfer_cancellation_reason unique (code),
    constraint uq_transfer_cancellation_reason_sequence_number unique (sequence_number)
)
;

with cancellation_reason as (
    (select *
     from (values ('OIC', 'Offence in custody', 100, true),
                  ('ADMI', 'Administrative', 110, true),
                  ('TRANS', 'Insufficient transport', 120, true))
              as t(code, description, sequence_number, active)))
insert
into transfer_cancellation_reason(code, description, sequence_number, active)
select code, description, sequence_number, active
from cancellation_reason
on conflict do nothing
;

alter table transfer
    add column if not exists cancellation_reason_id uuid
;

alter table transfer_audit
    add column if not exists cancellation_reason_id uuid
;

alter table transfer
    drop constraint if exists fk_transfer_cancellation_reason,
    add constraint fk_transfer_cancellation_reason foreign key (cancellation_reason_id) references transfer_cancellation_reason (id)
;

