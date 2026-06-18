alter table help_request add column if not exists start_latitude double precision;
alter table help_request add column if not exists start_longitude double precision;

alter table help_request drop constraint if exists help_request_start_location_valid;
alter table help_request add constraint help_request_start_location_valid check (
    (start_latitude is null and start_longitude is null) or
    (start_latitude between -90 and 90 and start_longitude between -180 and 180)
);

alter table task drop constraint if exists task_target_count_check;
alter table task add constraint task_target_count_check check (target_count in (1, 3, 5, 10));

update task
set status = 'ARCHIVED'
where activity_type = 'VOLUNTEER'
  and target_count in (5, 10)
  and status = 'ACTIVE';

create index if not exists ix_help_request_start_location
    on help_request(start_latitude, start_longitude);
