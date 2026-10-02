-- =====================================================================
-- V4 — Anamnese
-- =====================================================================

-- MVP: 1 modelo do sistema (organization_id nulo). V2: o profissional cria os próprios.
create table anamnesis_template (
  id               uuid primary key default uuid_generate_v7(),
  organization_id  uuid references organization(id),
  name             text not null,
  version          int not null,
  questions        jsonb not null,    -- [{code, label, type, options, required}]
  is_active        boolean not null default true,
  created_at       timestamptz not null default now(),
  -- nulls not distinct: também protege os modelos do sistema (organization_id nulo)
  unique nulls not distinct (organization_id, name, version)
);

-- Cada atualização = nova linha. Imutável depois de revisada (trigger abaixo).
create table anamnesis (
  id                 uuid primary key default uuid_generate_v7(),
  client_id          uuid not null references client(id),
  coaching_link_id   uuid,
  template_id        uuid not null references anamnesis_template(id),
  version_number     int not null,
  answers            jsonb not null,   -- {question_code: valor}
  goal               text,
  activity_level     text check (activity_level in ('sedentary','beginner','intermediate','advanced')),
  weekly_days        smallint check (weekly_days between 0 and 7),
  session_minutes    smallint check (session_minutes > 0),
  parq_positive      boolean not null default false,
  medical_clearance  text not null default 'not_required'
                     check (medical_clearance in ('not_required','pending','cleared')),
  clearance_date     date,
  filled_by          uuid not null references app_user(id),
  reviewed_by        uuid references app_user(id),
  reviewed_at        timestamptz,
  created_at         timestamptz not null default now(),
  unique (client_id, version_number),
  unique (id, client_id),
  foreign key (coaching_link_id, client_id) references coaching_link (id, client_id) on update cascade,
  check (medical_clearance <> 'cleared' or clearance_date is not null),
  check ((reviewed_by is null) = (reviewed_at is null))
);
create index on anamnesis (coaching_link_id);

-- Revisada = imutável (UPDATE e DELETE). Exceções controladas pelo sistema
-- (exclusão de conta no worker; troca de cadastro em accept_invite) usam
-- SET LOCAL app.system_change = 'on'.
create function forbid_change_when_reviewed() returns trigger
language plpgsql as $$
begin
  if old.reviewed_at is not null
     and coalesce(current_setting('app.system_change', true), '') <> 'on' then
    raise exception 'anamnesis revisada é imutável; crie uma nova versão';
  end if;
  return case when tg_op = 'DELETE' then old else new end;
end $$;

create trigger anamnesis_immutable before update or delete on anamnesis
  for each row execute function forbid_change_when_reviewed();

-- Lesões, cirurgias, dores e condições: o que gera aviso ao montar treino.
-- Ativa = resolved_on nulo (sem coluna is_active, para não divergir).
create table health_restriction (
  id                   uuid primary key default uuid_generate_v7(),
  client_id            uuid not null references client(id),
  source_anamnesis_id  uuid,
  kind                 text not null check (kind in ('injury','surgery','pain','condition')),
  body_region          body_region_t,
  description          text not null,
  severity             smallint check (severity between 1 and 3),
  resolved_on          date,
  created_by           uuid not null references app_user(id),
  created_at           timestamptz not null default now(),
  updated_at           timestamptz not null default now(),
  foreign key (source_anamnesis_id, client_id) references anamnesis (id, client_id) on update cascade
);
create index on health_restriction (client_id) where resolved_on is null;
create index on health_restriction (source_anamnesis_id);
