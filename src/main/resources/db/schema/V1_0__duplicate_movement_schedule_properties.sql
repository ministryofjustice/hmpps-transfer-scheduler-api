alter table transfer
    alter column reason_id drop not null
;

alter table transfer
    rename constraint pk_transfer_person to fk_transfer_person
;

alter table transfer
    rename constraint pk_transfer_status to fk_transfer_status
;

alter table transfer
    rename constraint pk_transfer_reason to fk_transfer_reason
;

alter table transfer
    rename constraint pk_transfer_logistics to fk_transfer_logistics
;

alter table transfer_audit
    alter column reason_id drop not null
;


drop table movement
;
drop table movement_audit
;

create table if not exists movement
(
    id               uuid       not null,
    version          int        not null,
    occurred_at      timestamp  not null,
    reason_id        uuid       not null,
    logistics_id     uuid       not null,
    destination_code varchar(6) not null,
    comments         text,
    legacy_id        varchar(32),
    constraint pk_movement primary key (id),
    constraint fk_movement_transfer foreign key (id) references transfer (id),
    constraint fk_movement_reason foreign key (reason_id) references transfer_reason (id),
    constraint fk_movement_logistics foreign key (logistics_id) references transfer_logistics (id),
    constraint uq_movement_legacy_id unique (legacy_id)
)
;

create table if not exists movement_audit
(
    rev_id           bigint     not null,
    rev_type         smallint   not null,
    id               uuid       not null,
    version          int        not null,
    occurred_at      timestamp  not null,
    reason_id        uuid       not null,
    logistics_id     uuid       not null,
    destination_code varchar(6) not null,
    comments         text,
    legacy_id        varchar(32)
)
;