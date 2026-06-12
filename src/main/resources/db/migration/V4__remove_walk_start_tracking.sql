alter table help_request
    drop column if exists requester_started_at,
    drop column if exists volunteer_started_at,
    drop column if exists started_at;
