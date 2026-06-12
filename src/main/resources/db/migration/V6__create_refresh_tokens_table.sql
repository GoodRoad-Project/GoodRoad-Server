create table if not exists refresh_tokens (
                                              id bigserial primary key,
                                              user_id bigint not null references users(id) on delete cascade,
    token varchar(500) not null unique,
    expires_at timestamp not null,
    revoked boolean default false,
    created_at timestamp default current_timestamp,
    device_info varchar(200)
    );

create index if not exists idx_refresh_tokens_user_id on refresh_tokens(user_id);
create index if not exists idx_refresh_tokens_token on refresh_tokens(token);
create index if not exists idx_refresh_tokens_expires_at on refresh_tokens(expires_at);