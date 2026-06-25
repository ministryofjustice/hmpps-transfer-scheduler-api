insert into reference_data_domain(code, description)
values ('TRANSFER_LOGISTICS', 'Transfer logistics'),
       ('TRANSFER_PRIORITY', 'Transfer priority'),
       ('TRANSFER_REASON', 'Transfer reason'),
       ('TRANSFER_STATUS', 'Transfer status')
;

with logistics as (
    (select *
     from (values ('A', 'Accompanied', 100, true),
                  ('L', 'Prison Officer Escort (Local)', 110, true),
                  ('N', 'Prison Officer Escort (National)', 120, true),
                  ('P', 'Police Escort', 130, true),
                  ('U', 'Unescorted', 140, true),
                  ('Z', 'Other (Immigration etc.)', 150, true),
                  ('GEOAME', 'GeoAmey', 160, true),
                  ('GROUP4', 'Group 4 Securicor', 170, true),
                  ('HMPS', 'H.M.P Establishment', 180, true),
                  ('PREM', 'Premier', 190, true),
                  ('PECS', 'Prisoner Escort Contractors (Other)', 200, true),
                  ('REL', 'Reliance', 210, true),
                  ('GEOAMEY', 'GeoAmey', 900, false))
              as t(code, description, sequence_number, active)))

insert
into transfer_logistics(code, description, sequence_number, active)
select code, description, sequence_number, active
from logistics on conflict do nothing;

with priority as (
    (select *
     from (values ('1', 'High', 10, true),
                  ('2', 'Medium', 20, true),
                  ('3', 'Low', 30, true))
              as t(code, description, sequence_number, active)))

insert
into transfer_priority(code, description, sequence_number, active)
select code, description, sequence_number, active
from priority on conflict do nothing;

with reason as (
    (select *
     from (values ('28', '28 Day Lie Down', 100, true),
                  ('ACCVISIT', 'Accumulated Visits', 110, true),
                  ('AS', 'Accumulated Visits with Stopover', 120, true),
                  ('APPEALS', 'Appeals', 130, true),
                  ('COMP', 'Compassionate Transfer', 140, true),
                  ('PROD', 'For Production', 150, true),
                  ('MED', 'Medical', 160, true),
                  ('NOTR', 'Normal Transfer', 170, true),
                  ('OTHER', 'Other inc Change Of Status', 180, true),
                  ('OJ', 'Outside Jurisdiction', 190, true),
                  ('OVCROW', 'Overcrowding Draft', 200, true),
                  ('PRES', 'Pre Release Employment Scheme', 210, true),
                  ('PROAT', 'Programme Attendance', 220, true),
                  ('SEC', 'Security Reasons', 230, true),
                  ('ADMIN', 'ADMINISTRATIVE', 900, false),
                  ('INT', 'Internal Transfer', 910, false),
                  ('TRN', 'Transfer', 920, false))
              as t(code, description, sequence_number, active)))

insert
into transfer_reason(code, description, sequence_number, active)
select code, description, sequence_number, active
from reason on conflict do nothing;

with status as (
    (select *
     from (values ('PLANNING', 'Awaiting details', 10, true),
                  ('READY_TO_SCHEDULE', 'Ready to schedule', 20, true),
                  ('SCHEDULED', 'Scheduled', 30, true),
                  ('CANCELLED', 'Cancelled', 40, true),
                  ('EXPIRED', 'Expired', 50, true),
                  ('IN_TRANSIT', 'In transit', 60, true),
                  ('COMPLETED', 'Completed', 70, true))
              as t(code, description, sequence_number, active)))

insert
into transfer_status(code, description, sequence_number, active)
select code, description, sequence_number, active
from status on conflict do nothing;