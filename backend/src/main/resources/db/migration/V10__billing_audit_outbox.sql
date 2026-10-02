-- =====================================================================
-- V10 — Cobrança, auditoria e outbox
-- =====================================================================

create table plan (
  id                  uuid primary key default uuid_generate_v7(),
  code                text not null unique,     -- free, personal, pro, studio
  name                text not null,
  max_active_clients  int not null check (max_active_clients >= 0),
  price_cents         int not null check (price_cents >= 0),
  billing_interval    text not null check (billing_interval in ('month','year')),
  is_active           boolean not null default true,
  created_at          timestamptz not null default now()
);

create table subscription (
  id                        uuid primary key default uuid_generate_v7(),
  organization_id           uuid not null references organization(id),
  plan_id                   uuid not null references plan(id),
  status                    text not null check (status in ('trialing','active','past_due','canceled')),
  provider                  text check (provider in ('mercado_pago','stripe')),
  provider_subscription_id  text,
  trial_ends_at             timestamptz,
  current_period_end        timestamptz,
  canceled_at               timestamptz,
  created_at                timestamptz not null default now(),
  updated_at                timestamptz not null default now(),
  unique (provider, provider_subscription_id),
  check ((status = 'canceled') = (canceled_at is not null)),
  check (status <> 'trialing' or trial_ends_at is not null)
);
-- No máximo uma assinatura viva por organização
create unique index one_live_subscription on subscription (organization_id)
  where status in ('trialing','active','past_due');

-- Webhooks do gateway: provider_event_id único evita processar duas vezes
create table payment_event (
  id                 uuid primary key default uuid_generate_v7(),
  subscription_id    uuid references subscription(id),
  provider           text not null check (provider in ('mercado_pago','stripe')),
  provider_event_id  text not null,
  type               text not null,
  payload            jsonb not null,
  processed_at       timestamptz,
  created_at         timestamptz not null default now(),
  unique (provider, provider_event_id)
);
create index on payment_event (subscription_id);

-- Auditoria de ACESSO a dado sensível (escopo enxuto).
-- Consentimento NÃO entra aqui (a tabela consent já é a trilha).
-- Append-only, particionada por mês, gravada na mesma transação do evento.
-- Não guardar valores de dado de saúde; só quais campos mudaram.
create table audit_log (
  id             bigint generated always as identity,
  created_at     timestamptz not null default now(),
  actor_id       uuid references app_user(id),
  entity         text not null,      -- 'progress_photo','anamnesis','app_user','coaching_link'
  entity_id      uuid not null,
  client_id      uuid,               -- sem FK: precisa sobreviver à anonimização
  action         text not null check (action in
                 ('view_photo','view_anamnesis','delete_account',
                  'link_created','link_changed','link_ended')),
  changed_fields text[],
  ip             inet,
  user_agent     text,
  request_id     uuid,
  primary key (id, created_at)
) partition by range (created_at);

-- Partições mensais. Em produção, pg_partman cria as futuras.
-- A default deve ficar VAZIA (monitorar): se cair linha nela, o pg_partman
-- falha ao criar a partição daquele mês.
create table audit_log_default partition of audit_log default;
create table audit_log_2026_10 partition of audit_log for values from ('2026-10-01') to ('2026-11-01');
create table audit_log_2026_11 partition of audit_log for values from ('2026-11-01') to ('2026-12-01');
create table audit_log_2026_12 partition of audit_log for values from ('2026-12-01') to ('2027-01-01');

create index on audit_log (client_id, created_at desc);
create index on audit_log (actor_id, created_at desc);
create index on audit_log (entity, entity_id, created_at desc);

-- Outbox: eventos de domínio gravados na mesma transação e processados pelo worker
-- (calcular PRs, gerar alertas, enviar push, thumbnails, exclusão de conta).
--
-- Worker (vários em paralelo sem pegar o mesmo evento):
--   select * from outbox_event
--    where processed_at is null and next_attempt_at <= now()
--    order by id
--    for update skip locked
--    limit 50;
-- Falha: attempts + 1, next_attempt_at = now() + backoff, last_error.
-- Limpeza: job diário apaga processados com mais de 30 dias.
create table outbox_event (
  id               bigint generated always as identity primary key,
  aggregate_type   text not null,      -- 'workout_session','progress_photo','app_user'...
  aggregate_id     uuid not null,
  type             text not null,      -- 'session.finished','photo.uploaded','account.deletion_requested'
  payload          jsonb not null,
  created_at       timestamptz not null default now(),
  next_attempt_at  timestamptz not null default now(),
  attempts         int not null default 0,
  processed_at     timestamptz,
  last_error       text
);
create index outbox_pending on outbox_event (next_attempt_at) where processed_at is null;
create index outbox_processed on outbox_event (processed_at) where processed_at is not null;
