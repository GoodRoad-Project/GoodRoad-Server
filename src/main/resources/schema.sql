create table if not exists users (
    id             uuid primary key,
    first_name     varchar(80),
    last_name      varchar(80),
    phone_hash     varchar(64) not null unique,
    role           varchar(16) not null
        check (role in ('USER', 'MODERATOR', 'MODERATOR_ADMIN')),
    password_hash  varchar(100) not null,
    photo_url      varchar(512),
    is_active      boolean not null default true,

    created_at     timestamptz not null default now(),
    last_active_at timestamptz not null default now()
);

create index if not exists ix_users_last_active_at on users(last_active_at);

create table if not exists obstacle_feature (
    id                uuid primary key,
    type              varchar(32) not null,
    latitude          double precision not null,
    longitude         double precision not null,
    severity_estimate smallint
        check (severity_estimate is null or (severity_estimate between 1 and 5)),
    reviews_count     integer not null default 0
        check (reviews_count >= 0),
    last_reviewed_at  timestamptz
);

create index if not exists ix_obstacle_feature_type on obstacle_feature(type);

create table if not exists user_obstacle_policy (
    user_id              uuid not null references users(id) on delete cascade,
    obstacle_type        varchar(32) not null,
    avoid                boolean not null,
    max_allowed_severity smallint
        check (max_allowed_severity is null or (max_allowed_severity between 1 and 5)),
    primary key (user_id, obstacle_type)
);

create table if not exists obstacle_review (
    id         uuid primary key,
    feature_id uuid not null references obstacle_feature(id) on delete cascade,
    author_id  uuid references users(id) on delete set null, -- отзывы сохраняем
    severity   smallint not null check (severity between 1 and 5),
    text       varchar(1000),
    created_at timestamptz not null default now(),
    status     varchar(16) not null
        check (status in ('PENDING', 'APPROVED', 'REJECTED')),
    constraint uq_obstacle_review_feature_author unique (feature_id, author_id)
);

create index if not exists ix_obstacle_review_feature on obstacle_review(feature_id);
create index if not exists ix_obstacle_review_status on obstacle_review(status);
create index if not exists ix_obstacle_review_feature_status_created
    on obstacle_review(feature_id, status, created_at desc);

create table if not exists obstacle_review_photo (
    id         uuid primary key,
    review_id  uuid not null references obstacle_review(id) on delete cascade,
    url        varchar(512) not null,
    created_at timestamptz not null default now()
);

create index if not exists ix_obstacle_review_photo_review on obstacle_review_photo(review_id);