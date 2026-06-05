create table application_field_values (
    id uuid primary key,
    application_id uuid not null references applications(id),
    field_key varchar(60) not null,
    label varchar(100) not null,
    data_type varchar(30) not null,
    value_text text,
    display_order integer not null,
    created_at timestamp with time zone not null default current_timestamp,
    unique (application_id, field_key)
);
