do
$$
    declare
        constraint_exists boolean;
    begin
        select exists (select 1
                       from information_schema.table_constraints
                       where table_schema = 'public'
                         and table_name = 'movement_audit'
                         and constraint_name = 'fk_movement_audit_revision')
        into constraint_exists;

        if constraint_exists = true then
            raise notice 'constraint already exists, skipping ...';
        else
            alter table movement_audit
                add constraint fk_movement_audit_revision foreign key (rev_id) references audit_revision (id)
            ;
        end if;
    end
$$;

do
$$
    declare
        constraint_exists boolean;
    begin
        select exists (select 1
                       from information_schema.table_constraints
                       where table_schema = 'public'
                         and table_name = 'plan_audit'
                         and constraint_name = 'fk_plan_audit_revision')
        into constraint_exists;

        if constraint_exists = true then
            raise notice 'constraint already exists, skipping ...';
        else
            alter table plan_audit
                add constraint fk_plan_audit_revision foreign key (rev_id) references audit_revision (id)
            ;
        end if;
    end
$$;

do
$$
    declare
        constraint_exists boolean;
    begin
        select exists (select 1
                       from information_schema.table_constraints
                       where table_schema = 'public'
                         and table_name = 'schedule_audit'
                         and constraint_name = 'fk_schedule_audit_revision')
        into constraint_exists;

        if constraint_exists = true then
            raise notice 'constraint already exists, skipping ...';
        else
            alter table schedule_audit
                add constraint fk_schedule_audit_revision foreign key (rev_id) references audit_revision (id)
            ;
        end if;
    end
$$;

do
$$
    declare
        constraint_exists boolean;
    begin
        select exists (select 1
                       from information_schema.table_constraints
                       where table_schema = 'public'
                         and table_name = 'transfer_audit'
                         and constraint_name = 'fk_transfer_audit_revision')
        into constraint_exists;

        if constraint_exists = true then
            raise notice 'constraint already exists, skipping ...';
        else
            alter table transfer_audit
                add constraint fk_transfer_audit_revision foreign key (rev_id) references audit_revision (id)
            ;
        end if;
    end
$$;

create index if not exists idx_movement_transfer_id on movement (transfer_id)
;

do
$$
    declare
        is_deferrable boolean;
    begin
        select condeferrable
        into is_deferrable
        from pg_constraint con
                 join pg_class cls on con.conrelid = cls.oid
                 join pg_namespace nsp on cls.relnamespace = nsp.oid
        where nsp.nspname = 'public'
          and cls.relname = 'movement'
          and con.conname = 'uq_movement_legacy_id';

        if is_deferrable = true then
            raise notice 'constraint uq_movement_legacy_id is already deferrable, skipping.';
        elsif is_deferrable = false then
            raise notice 'constraint is not deferrable, upgrading ...';

            alter table movement
                add constraint uq_movement_legacy_id_deferrable unique (legacy_id) deferrable initially deferred;

            alter table movement
                drop constraint if exists uq_movement_legacy_id;

            alter table movement
                rename constraint uq_movement_legacy_id_deferrable to uq_movement_legacy_id;
        else
            alter table movement
                add constraint uq_movement_legacy_id unique (legacy_id) deferrable initially deferred;
        end if;
    end
$$;

create index if not exists idx_movement_audit_legacy_id on movement_audit (legacy_id)
;

do
$$
    declare
        is_deferrable boolean;
    begin
        select condeferrable
        into is_deferrable
        from pg_constraint con
                 join pg_class cls on con.conrelid = cls.oid
                 join pg_namespace nsp on cls.relnamespace = nsp.oid
        where nsp.nspname = 'public'
          and cls.relname = 'transfer'
          and con.conname = 'uq_transfer_legacy_id';

        if is_deferrable = true then
            raise notice 'constraint uq_transfer_legacy_id is already deferrable, skipping.';
        elsif is_deferrable = false then
            raise notice 'constraint is not deferrable, upgrading ...';

            alter table transfer
                add constraint uq_transfer_legacy_id_deferrable unique (legacy_id) deferrable initially deferred;

            alter table transfer
                drop constraint if exists uq_transfer_legacy_id;

            alter table transfer
                rename constraint uq_transfer_legacy_id_deferrable to uq_transfer_legacy_id;
        else
            alter table transfer
                add constraint uq_transfer_legacy_id unique (legacy_id) deferrable initially deferred;
        end if;
    end
$$;

create index if not exists idx_transfer_audit_legacy_id on transfer_audit (legacy_id)
;

create index if not exists idx_transfer_prison_code_stage_status_id on transfer (prison_code, stage, status_id)
;

create index if not exists idx_schedule_start on schedule (start)
;

create table if not exists migration_system_audit
(
    id          uuid        not null,
    created_at  timestamp   not null,
    created_by  varchar(64) not null,
    modified_at timestamp,
    modified_by varchar(64),
    legacy_data jsonb,
    constraint pk_migration_system_audit primary key (id)
)
;