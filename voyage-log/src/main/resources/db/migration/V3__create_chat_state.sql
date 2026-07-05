create table if not exists chat_state (
    chat_id    bigint primary key,
    state      text        not null,
    payload    jsonb,
    updated_at timestamptz not null default now()
);
