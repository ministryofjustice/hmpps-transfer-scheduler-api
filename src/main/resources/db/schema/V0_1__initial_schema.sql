create table if not exists reference_data_domain
(
    code        varchar(32)  not null,
    description varchar(255) not null,
    constraint pk_reference_data_domain primary key (code)
)
;

create table if not exists transfer_logistics
(
    id              uuid         not null default uuidv7(),
    code            varchar(32)  not null,
    description     varchar(255) not null,
    sequence_number int          not null,
    active          boolean      not null,
    constraint pk_transfer_logistics primary key (id),
    constraint uq_transfer_logistics_code unique (code),
    constraint uq_transfer_logistics_sequence_number unique (sequence_number)
)
;

create table if not exists transfer_priority
(
    id              uuid         not null default uuidv7(),
    code            varchar(32)  not null,
    description     varchar(255) not null,
    sequence_number int          not null,
    active          boolean      not null,
    constraint pk_transfer_priority primary key (id),
    constraint uq_transfer_priority_code unique (code),
    constraint uq_transfer_priority_sequence_number unique (sequence_number)
)
;

create table if not exists transfer_reason
(
    id              uuid         not null default uuidv7(),
    code            varchar(32)  not null,
    description     varchar(255) not null,
    sequence_number int          not null,
    active          boolean      not null,
    constraint pk_transfer_reason primary key (id),
    constraint uq_transfer_reason_code unique (code),
    constraint uq_transfer_reason_sequence_number unique (sequence_number)
)
;

create table if not exists transfer_status
(
    id              uuid         not null default uuidv7(),
    code            varchar(32)  not null,
    description     varchar(255) not null,
    sequence_number int          not null,
    active          boolean      not null,
    constraint pk_transfer_status primary key (id),
    constraint uq_transfer_status_code unique (code),
    constraint uq_transfer_status_sequence_number unique (sequence_number)
)
;