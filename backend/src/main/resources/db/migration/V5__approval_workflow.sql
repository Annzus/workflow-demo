create table approval_tasks (
    id uuid primary key,
    application_id uuid not null references applications(id),
    approver_employee_id uuid not null references employees(id),
    approver_name varchar(100) not null,
    step_name varchar(100) not null,
    status varchar(30) not null,
    due_date date,
    completed_at timestamp with time zone,
    created_at timestamp with time zone not null default current_timestamp
);

create table approval_histories (
    id uuid primary key,
    application_id uuid not null references applications(id),
    actor_employee_id uuid not null references employees(id),
    actor_name varchar(100) not null,
    action varchar(30) not null,
    comment text,
    created_at timestamp with time zone not null default current_timestamp
);
