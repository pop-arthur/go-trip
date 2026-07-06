create extension if not exists pgcrypto;

with admin_config as (
  select
    nullif(trim('${gotrip.admin.email}'), '') as email,
    nullif('${gotrip.admin.password}', '') as password,
    coalesce(nullif(trim('${gotrip.admin.fullName}'), ''), 'GoTrip Admin') as full_name
),
admin_user as (
  insert into users (id, email, password_hash, full_name, created_at, updated_at)
  select gen_random_uuid(), email, crypt(password, gen_salt('bf', 12)), full_name, now(), now()
  from admin_config
  where email is not null and password is not null
  on conflict (email) do update set
    password_hash = excluded.password_hash,
    full_name = excluded.full_name,
    updated_at = now()
  returning id
)
insert into user_roles (user_id, role, created_at, updated_at)
select admin_user.id, roles.role, now(), now()
from admin_user
cross join (values ('USER'), ('ADMIN')) as roles(role)
on conflict do nothing;
