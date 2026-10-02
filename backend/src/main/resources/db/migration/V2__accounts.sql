-- =====================================================================
-- V2 — Contas
-- =====================================================================

-- id próprio; o vínculo com o provedor de login fica em auth_identity.
create table app_user (
  id             uuid primary key default uuid_generate_v7(),
  name           text not null,
  email          citext not null unique,          -- na anonimização: deleted+<id>@invalid
  phone          text,
  birth_date     date,
  sex            sex_t,
  avatar_path    text,
  locale         text not null default 'pt-BR',
  timezone       text not null default 'America/Sao_Paulo',
  weight_unit    text not null default 'kg' check (weight_unit in ('kg','lb')),
  length_unit    text not null default 'cm' check (length_unit in ('cm','in')),
  created_at     timestamptz not null default now(),
  updated_at     timestamptz not null default now(),
  deleted_at     timestamptz,                     -- pedido de exclusão
  anonymized_at  timestamptz,                     -- exclusão concluída
  check (anonymized_at is null or deleted_at is not null)
);

-- Um usuário pode entrar por Google, Apple, e-mail... e trocar de provedor
-- sem mudar o id interno. subject = "sub" do JWT daquele provedor.
create table auth_identity (
  provider    text not null,                      -- 'google','apple','password','keycloak'...
  subject     text not null,
  user_id     uuid not null references app_user(id),
  created_at  timestamptz not null default now(),
  last_login_at timestamptz,
  primary key (provider, subject)
);
create index on auth_identity (user_id);

-- MVP: 1 organização criada automaticamente por profissional
create table organization (
  id             uuid primary key default uuid_generate_v7(),
  name           text not null,
  owner_user_id  uuid not null references app_user(id),
  created_at     timestamptz not null default now()
);
create index on organization (owner_user_id);

create table organization_member (
  organization_id uuid not null references organization(id),
  user_id         uuid not null references app_user(id),
  role            text not null check (role in ('owner','admin','professional')),
  created_at      timestamptz not null default now(),
  primary key (organization_id, user_id)
);
create index on organization_member (user_id);

create table professional_profile (
  user_id          uuid primary key references app_user(id),
  profession       text not null default 'personal_trainer',  -- futuro: physio, nutritionist
  registry_number  text,                                      -- CREF
  bio              text,
  created_at       timestamptz not null default now()
);

-- LGPD: cada aceite/revogação, com a versão do texto aceito.
-- Esta tabela já é a trilha de consentimento (não duplicar no audit_log).
create table consent (
  id           uuid primary key default uuid_generate_v7(),
  user_id      uuid not null references app_user(id),
  kind         text not null check (kind in ('terms','privacy','health_data','photos')),
  doc_version  text not null,
  granted_at   timestamptz not null default now(),
  ip           inet,
  user_agent   text,
  revoked_at   timestamptz,
  revoked_ip   inet,
  created_at   timestamptz not null default now(),
  check (revoked_at is null or revoked_at >= granted_at)
);
create index on consent (user_id, kind) where revoked_at is null;
