drop table if exists movement
;
drop table if exists movement_audit
;

alter table transfer
    alter column reason_id set not null
;

alter table transfer_audit
    alter column reason_id set not null
;

create table if not exists movement
(
    id               uuid       not null,
    version          int        not null,
    transfer_id      uuid       not null,
    occurred_at      timestamp  not null,
    reason_id        uuid       not null,
    logistics_id     uuid       not null,
    destination_code varchar(6) not null,
    comments         text,
    legacy_id        varchar(32),
    constraint pk_movement primary key (id),
    constraint uq_movement_transfer unique (transfer_id),
    constraint fk_movement_transfer foreign key (transfer_id) references transfer (id),
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
    transfer_id      uuid       not null,
    occurred_at      timestamp  not null,
    reason_id        uuid       not null,
    logistics_id     uuid       not null,
    destination_code varchar(6) not null,
    comments         text,
    legacy_id        varchar(32)
)
;