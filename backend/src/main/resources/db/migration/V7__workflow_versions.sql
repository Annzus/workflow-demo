create table workflow_versions (
    id uuid primary key,
    workflow_definition_id uuid not null references workflow_definitions(id),
    version_number integer not null,
    published boolean not null default false,
    created_at timestamp with time zone not null default current_timestamp,
    unique (workflow_definition_id, version_number)
);

create table workflow_nodes (
    id uuid primary key,
    workflow_version_id uuid not null references workflow_versions(id),
    node_key varchar(60) not null,
    node_name varchar(100) not null,
    node_type varchar(30) not null,
    approver_type varchar(40),
    position_code varchar(20),
    employee_code varchar(20),
    display_order integer not null,
    x_position integer not null,
    y_position integer not null,
    unique (workflow_version_id, node_key)
);

create table workflow_edges (
    id uuid primary key,
    workflow_version_id uuid not null references workflow_versions(id),
    source_node_key varchar(60) not null,
    target_node_key varchar(60) not null,
    condition_expression varchar(200),
    display_order integer not null
);

insert into workflow_versions (
    id,
    workflow_definition_id,
    version_number,
    published
) values
    ('93000000-0000-0000-0000-000000000001', '90000000-0000-0000-0000-000000000001', 1, true),
    ('93000000-0000-0000-0000-000000000002', '90000000-0000-0000-0000-000000000002', 1, true);

insert into workflow_nodes (
    id,
    workflow_version_id,
    node_key,
    node_name,
    node_type,
    approver_type,
    position_code,
    employee_code,
    display_order,
    x_position,
    y_position
) values
    ('94000000-0000-0000-0000-000000000001', '93000000-0000-0000-0000-000000000001', 'applicant', '申請者', 'APPLICANT', null, null, null, 10, 80, 90),
    ('94000000-0000-0000-0000-000000000002', '93000000-0000-0000-0000-000000000001', 'manager_approval', '部長承認', 'APPROVAL', 'FIXED_EMPLOYEE', null, '1005', 20, 280, 90),
    ('94000000-0000-0000-0000-000000000003', '93000000-0000-0000-0000-000000000001', 'finish', '完了', 'END', null, null, null, 30, 480, 90),
    ('94000000-0000-0000-0000-000000000011', '93000000-0000-0000-0000-000000000002', 'applicant', '申請者', 'APPLICANT', null, null, null, 10, 80, 90),
    ('94000000-0000-0000-0000-000000000012', '93000000-0000-0000-0000-000000000002', 'general_affairs', '総務確認', 'APPROVAL', 'FIXED_EMPLOYEE', null, '1005', 20, 280, 90),
    ('94000000-0000-0000-0000-000000000013', '93000000-0000-0000-0000-000000000002', 'finish', '完了', 'END', null, null, null, 30, 480, 90);

insert into workflow_edges (
    id,
    workflow_version_id,
    source_node_key,
    target_node_key,
    condition_expression,
    display_order
) values
    ('95000000-0000-0000-0000-000000000001', '93000000-0000-0000-0000-000000000001', 'applicant', 'manager_approval', null, 10),
    ('95000000-0000-0000-0000-000000000002', '93000000-0000-0000-0000-000000000001', 'manager_approval', 'finish', 'approved', 20),
    ('95000000-0000-0000-0000-000000000011', '93000000-0000-0000-0000-000000000002', 'applicant', 'general_affairs', null, 10),
    ('95000000-0000-0000-0000-000000000012', '93000000-0000-0000-0000-000000000002', 'general_affairs', 'finish', 'approved', 20);
