alter table task add column if not exists assigned_user_id bigint references users(id) on delete cascade;

alter table task_target add column if not exists status varchar(16) not null default 'ACTIVE';

alter table task_target drop constraint if exists task_target_status_check;
alter table task_target add constraint task_target_status_check check (status in ('ACTIVE', 'UNAVAILABLE'));

-- Active generated tasks can now be personal, so several users may have tasks with the same title.
drop index if exists uq_task_title_active;
create index if not exists ix_task_assigned_user_activity_status
    on task(assigned_user_id, activity_type, status);
create index if not exists ix_task_target_task_status
    on task_target(task_id, status, sort_order);
