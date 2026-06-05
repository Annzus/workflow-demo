create table application_form_fields (
    id uuid primary key,
    form_definition_id uuid not null references application_form_definitions(id),
    field_key varchar(60) not null,
    label varchar(100) not null,
    data_type varchar(30) not null,
    required boolean not null default false,
    placeholder varchar(200),
    initial_value_type varchar(40) not null default 'NONE',
    display_order integer not null,
    active boolean not null default true,
    created_at timestamp with time zone not null default current_timestamp,
    unique (form_definition_id, field_key)
);

insert into workflow_definitions (
    id,
    workflow_code,
    workflow_name
) values
    ('90000000-0000-0000-0000-000000000001', 'WF-DEPT-APPROVAL', '部門承認ルート'),
    ('90000000-0000-0000-0000-000000000002', 'WF-GENERAL-AFFAIRS', '総務確認ルート');

insert into application_form_definitions (
    id,
    form_code,
    form_name,
    workflow_definition_id
) values
    ('91000000-0000-0000-0000-000000000001', 'TRAVEL', '出張申請', '90000000-0000-0000-0000-000000000001'),
    ('91000000-0000-0000-0000-000000000002', 'PURCHASE', '備品購入稟議', '90000000-0000-0000-0000-000000000001'),
    ('91000000-0000-0000-0000-000000000003', 'TIMESHEET', '月次勤務表', '90000000-0000-0000-0000-000000000002');

insert into application_form_fields (
    id,
    form_definition_id,
    field_key,
    label,
    data_type,
    required,
    placeholder,
    initial_value_type,
    display_order
) values
    ('92000000-0000-0000-0000-000000000001', '91000000-0000-0000-0000-000000000001', 'destination', '出張先', 'TEXT', true, '例：東京本社', 'NONE', 10),
    ('92000000-0000-0000-0000-000000000002', '91000000-0000-0000-0000-000000000001', 'start_date', '開始日', 'DATE', true, null, 'NONE', 20),
    ('92000000-0000-0000-0000-000000000003', '91000000-0000-0000-0000-000000000001', 'end_date', '終了日', 'DATE', true, null, 'NONE', 30),
    ('92000000-0000-0000-0000-000000000004', '91000000-0000-0000-0000-000000000001', 'purpose', '目的', 'TEXTAREA', true, '出張の目的を入力', 'NONE', 40),
    ('92000000-0000-0000-0000-000000000005', '91000000-0000-0000-0000-000000000001', 'estimated_cost', '概算費用', 'NUMBER', false, '0', 'NONE', 50),

    ('92000000-0000-0000-0000-000000000011', '91000000-0000-0000-0000-000000000002', 'item_name', '購入品名', 'TEXT', true, '例：業務用ノートPC', 'NONE', 10),
    ('92000000-0000-0000-0000-000000000012', '91000000-0000-0000-0000-000000000002', 'vendor', '購入先', 'TEXT', false, '見積先または販売店', 'NONE', 20),
    ('92000000-0000-0000-0000-000000000013', '91000000-0000-0000-0000-000000000002', 'amount', '金額', 'NUMBER', true, '0', 'NONE', 30),
    ('92000000-0000-0000-0000-000000000014', '91000000-0000-0000-0000-000000000002', 'reason', '購入理由', 'TEXTAREA', true, '必要性を入力', 'NONE', 40),

    ('92000000-0000-0000-0000-000000000021', '91000000-0000-0000-0000-000000000003', 'target_month', '対象月', 'MONTH', true, null, 'CURRENT_MONTH', 10),
    ('92000000-0000-0000-0000-000000000022', '91000000-0000-0000-0000-000000000003', 'overtime_hours', '残業時間', 'NUMBER', false, '0', 'NONE', 20),
    ('92000000-0000-0000-0000-000000000023', '91000000-0000-0000-0000-000000000003', 'comment', '備考', 'TEXTAREA', false, '特記事項を入力', 'NONE', 30);
