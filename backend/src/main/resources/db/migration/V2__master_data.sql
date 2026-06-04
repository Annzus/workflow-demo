create table organizations (
    id uuid primary key,
    organization_code varchar(20) not null unique,
    name varchar(100) not null,
    parent_organization_code varchar(20),
    valid_from date not null,
    valid_to date not null,
    active boolean not null default true,
    created_at timestamp with time zone not null default current_timestamp
);

create table positions (
    id uuid primary key,
    position_code varchar(20) not null unique,
    name varchar(100) not null,
    approval_rank integer not null,
    active boolean not null default true,
    created_at timestamp with time zone not null default current_timestamp
);

insert into organizations (
    id,
    organization_code,
    name,
    parent_organization_code,
    valid_from,
    valid_to
) values
    ('11111111-1111-1111-1111-111111111111', '01000000', 'ソリューション事業本部', null, '2015-01-01', '2999-12-31'),
    ('22222222-2222-2222-2222-222222222222', '01010000', '第１ソリューション部', '01000000', '2015-01-01', '2999-12-31'),
    ('33333333-3333-3333-3333-333333333333', '01010110', '第１グループ', '01010000', '2015-01-01', '2999-12-31'),
    ('44444444-4444-4444-4444-444444444444', '01010120', '第２グループ', '01010000', '2015-01-01', '2999-12-31'),
    ('55555555-5555-5555-5555-555555555555', '01020000', '第２ソリューション部', '01000000', '2015-01-01', '2999-12-31');

insert into positions (
    id,
    position_code,
    name,
    approval_rank
) values
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '110', 'サブリーダー', 110),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '120', 'リーダー', 120),
    ('cccccccc-cccc-cccc-cccc-cccccccccccc', '130', '課長', 130),
    ('dddddddd-dddd-dddd-dddd-dddddddddddd', '140', '部長代理', 140),
    ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', '150', '部長', 150),
    ('ffffffff-ffff-ffff-ffff-ffffffffffff', '160', '事業本部長', 160);

insert into employees (
    id,
    employee_code,
    name,
    email,
    organization_name,
    position_name
) values
    ('10010000-0000-0000-0000-000000000001', '1001', '山田 太郎', 'demo1@example.local', '第１グループ', '課長'),
    ('10020000-0000-0000-0000-000000000002', '1002', '柳田 雅之', 'demo2@example.local', '第１ソリューション部', 'サブリーダー'),
    ('10030000-0000-0000-0000-000000000003', '1003', '久米 幸子', 'demo3@example.local', '第２グループ', 'リーダー'),
    ('10050000-0000-0000-0000-000000000005', '1005', '岩瀬 大樹', 'demo5@example.local', '第１ソリューション部', '部長'),
    ('10070000-0000-0000-0000-000000000007', '1007', '岩村 亮', 'demo7@example.local', '第２ソリューション部', '部長');
