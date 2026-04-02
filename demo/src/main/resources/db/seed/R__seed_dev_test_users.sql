insert into roles (code, name)
values ('ADMIN', 'Administrator'),
       ('USER', 'User')
on conflict (code) do nothing;

insert into users (username, password_hash, enabled)
values ('admin', '$2b$12$SLm3Aa9G3FIGT4RDw8GO3uzFW0wUIZ4SPOSwTvdpu3AiaBo9oxAl2', true),
       ('user01', '$2b$12$Fe353t28P9WJGKdnxR1kGehkiGWqoK/ZXdOaqkqFoEG1dHdr6ctai', true)
on conflict (username) do nothing;

insert into user_roles (user_id, role_id)
select users.id, roles.id
from users
join roles on roles.code = 'ADMIN'
where users.username = 'admin'
on conflict (user_id, role_id) do nothing;

insert into user_roles (user_id, role_id)
select users.id, roles.id
from users
join roles on roles.code = 'USER'
where users.username = 'user01'
on conflict (user_id, role_id) do nothing;
