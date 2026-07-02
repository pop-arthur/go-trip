create table auth_sessions (
  id                 uuid primary key,
  user_id            uuid not null references users(id) on delete cascade,
  refresh_token_hash text not null,
  expires_at         timestamptz not null,
  revoked_at         timestamptz,
  created_at         timestamptz not null default now(),
  updated_at         timestamptz not null default now()
);

create index idx_auth_sessions_user_id on auth_sessions(user_id);
create index idx_auth_sessions_expires_at on auth_sessions(expires_at);
create index idx_auth_sessions_active on auth_sessions(user_id, expires_at)
  where revoked_at is null;

create unique index uq_user_roles_user_role on user_roles(user_id, role);
