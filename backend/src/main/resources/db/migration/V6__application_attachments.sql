create table application_attachments (
    id uuid primary key,
    application_id uuid not null references applications(id),
    original_filename varchar(255) not null,
    object_key varchar(512) not null,
    content_type varchar(150) not null,
    size_bytes bigint not null,
    uploaded_by_employee_id uuid not null references employees(id),
    uploaded_by_name varchar(100) not null,
    uploaded_at timestamp with time zone not null default current_timestamp
);

create index idx_application_attachments_application_id
    on application_attachments(application_id);
