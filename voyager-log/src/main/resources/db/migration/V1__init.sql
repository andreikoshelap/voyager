create table skipper (
    id               bigserial primary key,
    telegram_chat_id bigint      not null unique,
    name             text        not null,
    phone            text,
    created_at       timestamptz not null default now()
);

create table vessel (
    id          bigserial primary key,
    skipper_id  bigint not null references skipper (id),
    name        text   not null,
    type        text,
    length_m    numeric(4, 1),
    sail_number text
);

create table emergency_contact (
    id               bigserial primary key,
    skipper_id       bigint not null references skipper (id),
    name             text   not null,
    telegram_chat_id bigint,
    phone            text
);

create table harbour (
    id               bigserial primary key,
    name             text not null,
    lat              double precision,
    lon              double precision,
    vhf_channel      text,
    phone            text,
    telegram_chat_id bigint,
    depth_m          numeric(4, 1),
    price_note       text
);

create table trip (
    id                   bigserial primary key,
    skipper_id           bigint      not null references skipper (id),
    vessel_id            bigint references vessel (id),
    departure_harbour_id bigint references harbour (id),
    destination          text        not null,
    crew_count           int         not null default 1,
    departed_at          timestamptz not null,
    eta_return           timestamptz not null,
    status               text        not null,
    overdue_at           timestamptz,
    alerted_at           timestamptz,
    version              bigint      not null default 0
);

create index idx_trip_status_eta on trip (status, eta_return);
create index idx_trip_status_overdue on trip (status, overdue_at);

-- Multi-step dialog state (FSM); Telegram itself is stateless
create table chat_state (
    chat_id    bigint primary key,
    state      text        not null,
    payload    jsonb,
    updated_at timestamptz not null default now()
);

-- ShedLock housekeeping table
create table shedlock (
    name       varchar(64) primary key,
    lock_until timestamptz  not null,
    locked_at  timestamptz  not null,
    locked_by  varchar(255) not null
);
