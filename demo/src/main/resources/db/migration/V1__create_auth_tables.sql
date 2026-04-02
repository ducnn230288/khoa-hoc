create table users (
    id bigserial primary key,
    username varchar(255) not null unique,
    password_hash varchar(255) not null,
    enabled boolean not null default true,
    created_at timestamptz not null default current_timestamp,
    updated_at timestamptz not null default current_timestamp
);

create table roles (
    id bigserial primary key,
    code varchar(100) not null unique,
    name varchar(255) not null
);

create table user_roles (
    user_id bigint not null,
    role_id bigint not null,
    primary key (user_id, role_id),
    constraint fk_user_roles_user foreign key (user_id) references users (id),
    constraint fk_user_roles_role foreign key (role_id) references roles (id)
);
