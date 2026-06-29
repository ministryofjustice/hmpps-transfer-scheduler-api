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

create table if not exists hmpps_domain_event
(
    id         uuid    not null,
    version    int     not null,
    entity_id  uuid    not null,
    event_type text    not null,
    event      jsonb   not null,
    published  boolean not null,
    constraint pk_hmpps_domain_event primary key (id)
)
;

create index if not exists idx_hmpps_domain_event_unpublished on hmpps_domain_event (id) where (published = false);

create table if not exists person_summary
(
    person_identifier varchar(10) not null,
    version           int         not null,
    first_name        varchar(64) not null,
    last_name         varchar(64) not null,
    prison_code       varchar(6),
    cell_location     varchar(64),
    constraint pk_person_summary primary key (person_identifier)
)
;

create index if not exists idx_person_summary_name on person_summary (lower(last_name::text), lower(first_name::text));

create table if not exists audit_revision
(
    id                bigserial   not null,
    timestamp         timestamp   not null,
    source            varchar(6)  not null,
    affected_entities text[]      not null,
    username          varchar(64) not null,
    caseload_id       varchar(10),
    reason            text,
    constraint pk_audit_revision primary key (id),
    constraint ch_audit_revision_source check (source in ('DPS', 'NOMIS'))
)
;

create table if not exists hmpps_domain_event_audit
(
    rev_id     bigint   not null,
    rev_type   smallint not null,
    id         uuid     not null,
    version    int      not null,
    entity_id  uuid     not null,
    event_type text     not null,
    event      jsonb    not null,
    published  boolean  not null,
    constraint pk_hmpps_domain_event_audit primary key (id, rev_id),
    constraint fk_hmpps_domain_event_audit_revision foreign key (rev_id) references audit_revision (id)
)
;

create index if not exists idx_hmpps_domain_event_audit_event_type_entity_id on hmpps_domain_event_audit (event_type, entity_id);

create table if not exists transfer
(
    id                uuid        not null,
    version           int         not null,
    person_identifier varchar(10) not null,
    prison_code       varchar(6)  not null,
    status_id         uuid        not null,
    reason_id         uuid        not null,
    logistics_id      uuid,
    destination_code  varchar(6),
    comments          text,
    legacy_id         bigint,
    constraint pk_transfer primary key (id),
    constraint pk_transfer_person foreign key (person_identifier) references person_summary (person_identifier),
    constraint pk_transfer_status foreign key (status_id) references transfer_status (id),
    constraint pk_transfer_reason foreign key (reason_id) references transfer_reason (id),
    constraint pk_transfer_logistics foreign key (logistics_id) references transfer_logistics (id),
    constraint uq_transfer_legacy_id unique (legacy_id)
)
;

create table if not exists plan
(
    id           uuid not null,
    version      int  not null,
    requested_on date not null,
    priority_id  uuid not null,
    comments     text,
    constraint pk_plan primary key (id),
    constraint fk_plan_transfer foreign key (id) references transfer (id),
    constraint fk_plan_priority foreign key (priority_id) references transfer_priority (id)
)
;

create table if not exists schedule
(
    id       uuid      not null,
    version  int       not null,
    start    timestamp not null,
    comments text,
    constraint pk_schedule primary key (id),
    constraint fk_schedule_transfer foreign key (id) references transfer (id)
)
;

create table if not exists movement
(
    id          uuid      not null,
    version     int       not null,
    occurred_at timestamp not null,
    comments    text,
    legacy_id   varchar(32),
    constraint pk_movement primary key (id),
    constraint fk_movement_transfer foreign key (id) references transfer (id),
    constraint uq_movement_legacy_id unique (legacy_id)
)
;

create table if not exists transfer_audit
(
    rev_id            bigint      not null,
    rev_type          smallint    not null,
    id                uuid        not null,
    version           int         not null,
    person_identifier varchar(10) not null,
    prison_code       varchar(6)  not null,
    status_id         uuid        not null,
    reason_id         uuid        not null,
    logistics_id      uuid,
    destination_code  varchar(6),
    comments          text,
    legacy_id         bigint
)
;

create table if not exists plan_audit
(
    rev_id       bigint   not null,
    rev_type     smallint not null,
    id           uuid     not null,
    version      int      not null,
    requested_on date     not null,
    priority_id  uuid     not null,
    comments     text
)
;

create table if not exists schedule_audit
(
    rev_id   bigint    not null,
    rev_type smallint  not null,
    id       uuid      not null,
    version  int       not null,
    start    timestamp not null,
    comments text
)
;

create table if not exists movement_audit
(
    rev_id      bigint    not null,
    rev_type    smallint  not null,
    id          uuid      not null,
    version     int       not null,
    occurred_at timestamp not null,
    comments    text,
    legacy_id   varchar(32)
)
;