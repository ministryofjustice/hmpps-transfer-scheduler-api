create index if not exists idx_schedule_start on schedule (start);
create index if not exists idx_person_summary_prison_code_person_identifier on person_summary (prison_code, person_identifier);
create index if not exists idx_transfer_prison_code_person_identifier on transfer (prison_code, person_identifier);
