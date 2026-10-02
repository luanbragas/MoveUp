-- =====================================================================
-- V3 — Clientes e vínculo
-- =====================================================================

create table client (
  id          uuid primary key default uuid_generate_v7(),
  user_id     uuid unique references app_user(id),   -- nulo até aceitar o convite
  name        text not null,
  email       citext,
  phone       text,
  birth_date  date,
  sex         sex_t,
  created_by  uuid references app_user(id),
  created_at  timestamptz not null default now(),
  updated_at  timestamptz not null default now()
);

create table coaching_link (
  id               uuid primary key default uuid_generate_v7(),
  organization_id  uuid not null references organization(id),
  professional_id  uuid not null references app_user(id),
  client_id        uuid not null references client(id),
  status           text not null check (status in ('pending','active','inactive','ended')),
  goal             text,
  started_at       timestamptz,
  ended_at         timestamptz,
  created_at       timestamptz not null default now(),
  updated_at       timestamptz not null default now(),
  -- alvo das FKs compostas: garante que client_id das tabelas filhas
  -- é o mesmo cliente do vínculo (o RLS depende disso)
  unique (id, client_id),
  check (status <> 'active' or started_at is not null),
  check (status <> 'ended'  or ended_at   is not null)
);
-- MVP: um vínculo ativo por cliente
create unique index one_active_coaching_link on coaching_link (client_id) where status = 'active';
create index on coaching_link (professional_id, status);
create index on coaching_link (organization_id, status);

create table invite (
  id                uuid primary key default uuid_generate_v7(),
  coaching_link_id  uuid not null references coaching_link(id),
  code              text not null unique,            -- código curto / QR (gerar com CSPRNG)
  expires_at        timestamptz not null,
  accepted_at       timestamptz,
  accepted_by       uuid references app_user(id),
  revoked_at        timestamptz,                     -- profissional cancelou / reenviou
  created_at        timestamptz not null default now(),
  check (accepted_at is null or revoked_at is null)
);
create index on invite (coaching_link_id);

-- Anotações privadas do profissional (o aluno não vê)
create table client_note (
  id                uuid primary key default uuid_generate_v7(),
  coaching_link_id  uuid not null references coaching_link(id),
  author_id         uuid not null references app_user(id),
  body              text not null,
  created_at        timestamptz not null default now()
);
create index on client_note (coaching_link_id, created_at desc);
