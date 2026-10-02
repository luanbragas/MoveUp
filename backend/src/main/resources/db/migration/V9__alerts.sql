-- =====================================================================
-- V9 — Atenção (alertas e push)
-- =====================================================================

create table alert (
  id               uuid primary key default uuid_generate_v7(),
  organization_id  uuid not null references organization(id),
  professional_id  uuid not null references app_user(id),
  client_id        uuid not null references client(id),
  type             alert_type_t not null,
  severity         text not null check (severity in ('info','warning','urgent')),
  facts            jsonb not null,    -- só números que explicam o alerta; nunca dado clínico detalhado
  dedupe_key       text not null,     -- ex.: inactive:<client_id>
  status           text not null default 'open' check (status in ('open','resolved','snoozed')),
  snoozed_until    timestamptz,
  resolved_at      timestamptz,
  created_at       timestamptz not null default now(),
  updated_at       timestamptz not null default now(),
  check ((status = 'snoozed') = (snoozed_until is not null)),
  check ((status = 'resolved') = (resolved_at is not null))
);
-- Dedupe por profissional: se o aluno troca de personal, o alerta do novo
-- não é bloqueado pelo alerta aberto do antigo.
create unique index one_open_alert on alert (professional_id, dedupe_key) where status <> 'resolved';
create index on alert (professional_id, status, created_at desc);
create index on alert (client_id);

create table alert_setting (
  professional_id  uuid not null references app_user(id),
  type             alert_type_t not null,
  enabled          boolean not null default true,
  push_enabled     boolean not null default false,
  threshold        jsonb,             -- {"days": 7}
  primary key (professional_id, type)
);

create table push_device (
  id            uuid primary key default uuid_generate_v7(),
  user_id       uuid not null references app_user(id),
  platform      text not null check (platform in ('ios','android','web')),
  token         text not null unique,
  last_seen_at  timestamptz,
  created_at    timestamptz not null default now()
);
create index on push_device (user_id);
