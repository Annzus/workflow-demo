create table employees (
    id uuid primary key,
    employee_code varchar(20) not null unique,
    name varchar(100) not null,
    email varchar(255) not null unique,
    organization_name varchar(100) not null,
    position_name varchar(100) not null,
    active boolean not null default true,
    created_at timestamp with time zone not null default current_timestamp
);

create table workflow_definitions (
    id uuid primary key,
    workflow_code varchar(20) not null unique,
    workflow_name varchar(100) not null,
    active boolean not null default true,
    created_at timestamp with time zone not null default current_timestamp
);

create table application_form_definitions (
    id uuid primary key,
    form_code varchar(20) not null unique,
    form_name varchar(100) not null,
    workflow_definition_id uuid references workflow_definitions(id),
    active boolean not null default true,
    created_at timestamp with time zone not null default current_timestamp
);

create table applications (
    id uuid primary key,
    application_number varchar(40) not null unique,
    form_definition_id uuid not null references application_form_definitions(id),
    title varchar(200) not null,
    applicant_employee_id uuid not null references employees(id),
    status varchar(30) not null,
    submitted_at timestamp with time zone,
    created_at timestamp with time zone not null default current_timestamp
);
