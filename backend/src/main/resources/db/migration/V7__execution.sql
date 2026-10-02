-- =====================================================================
-- V7 — Execução (o realizado)
-- IDs destas tabelas são gerados no celular (UUIDv7) e enviados pelo app:
-- o sync faz upsert por id, então reenvio offline é idempotente em todas
-- as tabelas (não só na sessão). client_updated_at = hora da edição no
-- aparelho, usada para "última edição vence".
-- client_id é repetido em todas as filhas: protegido por FK composta e
-- usado direto pelo RLS e pelos gráficos.
-- =====================================================================

create table workout_session (
  id                     uuid primary key,          -- gerado no app
  client_id              uuid not null references client(id),
  coaching_link_id       uuid not null,
  program_id             uuid,
  workout_id             uuid,
  workout_version_id     uuid,                       -- o planejado daquele dia
  status                 text not null check (status in ('in_progress','completed','partial','abandoned')),
  started_at             timestamptz not null,
  finished_at            timestamptz,
  duration_seconds       int check (duration_seconds >= 0),
  completion_ratio       numeric(4,3) check (completion_ratio between 0 and 1),  -- base da adesão
  performed_by           text not null check (performed_by in ('client','professional')),
  performed_by_user      uuid not null references app_user(id),
  source                 text not null default 'app' check (source in ('app','healthkit','health_connect')),
  edited_after_finish_at timestamptz,
  client_updated_at      timestamptz not null,
  created_at             timestamptz not null default now(),
  updated_at             timestamptz not null default now(),
  unique (id, client_id),
  foreign key (coaching_link_id, client_id)   references coaching_link (id, client_id) on update cascade,
  foreign key (program_id, client_id)         references program (id, client_id) on update cascade,
  foreign key (workout_id, workout_version_id) references workout_version (workout_id, id),
  check (status not in ('completed','partial') or finished_at is not null),
  check (finished_at is null or finished_at >= started_at),
  check ((workout_id is null) = (workout_version_id is null))
);
create index on workout_session (client_id, started_at desc);
create index on workout_session (coaching_link_id, started_at desc);
create index on workout_session (workout_version_id);

create table performed_exercise (
  id                      uuid primary key,         -- gerado no app
  session_id              uuid not null,
  client_id               uuid not null,
  prescribed_exercise_id  uuid references prescribed_exercise(id),   -- nulo = extra
  exercise_id             uuid not null references exercise(id),
  position                smallint not null,
  status                  text not null check (status in ('done','skipped','substituted')),
  substituted_from        uuid references exercise(id),
  notes                   text,
  client_updated_at       timestamptz not null,
  updated_at              timestamptz not null default now(),
  unique (id, client_id),
  foreign key (session_id, client_id) references workout_session (id, client_id) on delete cascade,
  check ((status = 'substituted') = (substituted_from is not null))
);
create index on performed_exercise (session_id);
create index on performed_exercise (client_id, exercise_id);

create table performed_set (
  id                     uuid primary key,          -- gerado no app
  performed_exercise_id  uuid not null,
  client_id              uuid not null,
  prescribed_set_id      uuid references prescribed_set(id),
  set_number             smallint not null check (set_number >= 1),
  set_type               set_type_t not null default 'normal',
  side                   text check (side in ('left','right')),       -- unilateral
  reps                   smallint check (reps >= 0),
  load_kg                numeric(7,3) check (load_kg >= 0),
  duration_seconds       int check (duration_seconds >= 0),
  distance_m             int check (distance_m >= 0),
  rpe                    numeric(3,1) check (rpe between 0 and 10),
  rir                    smallint check (rir between 0 and 10),
  completed              boolean not null default true,
  completed_at           timestamptz,
  client_updated_at      timestamptz not null,
  updated_at             timestamptz not null default now(),
  foreign key (performed_exercise_id, client_id)
    references performed_exercise (id, client_id) on delete cascade
);
create index on performed_set (performed_exercise_id, set_number);

-- Resultado de bloco por tempo: rounds no AMRAP, tempo total no circuito
create table block_result (
  session_id        uuid not null,
  workout_block_id  uuid not null references workout_block(id),
  client_id         uuid not null,
  rounds_completed  smallint check (rounds_completed >= 0),
  extra_reps        smallint check (extra_reps >= 0),
  total_seconds     int check (total_seconds >= 0),
  notes             text,
  client_updated_at timestamptz not null,
  primary key (session_id, workout_block_id),
  foreign key (session_id, client_id) references workout_session (id, client_id) on delete cascade
);

create table session_feedback (
  session_id          uuid primary key,
  client_id           uuid not null,
  effort              smallint not null check (effort between 0 and 10),
  comment             text,
  has_pain            boolean not null default false,
  read_at             timestamptz,
  professional_reply  text,
  replied_at          timestamptz,
  created_at          timestamptz not null default now(),
  updated_at          timestamptz not null default now(),
  unique (session_id, client_id),
  foreign key (session_id, client_id) references workout_session (id, client_id) on delete cascade,
  check ((professional_reply is null) = (replied_at is null))
);

create table pain_report (
  id           uuid primary key,                    -- gerado no app
  session_id   uuid not null,
  client_id    uuid not null,
  body_region  body_region_t not null,
  exercise_id  uuid references exercise(id),
  intensity    smallint check (intensity between 0 and 10),
  description  text,
  foreign key (session_id, client_id) references session_feedback (session_id, client_id) on delete cascade
);
create index on pain_report (session_id);

-- Recordes pessoais: cache calculado ao finalizar a sessão (worker via outbox).
-- Histórico mantido (is_current = false nos superados) → linha do tempo de PRs.
-- best_pace: menor é melhor; os demais: maior é melhor.
create table personal_record (
  id               uuid primary key default uuid_generate_v7(),
  client_id        uuid not null,
  exercise_id      uuid not null references exercise(id),
  record_type      text not null check (record_type in
                   ('max_load','max_reps_at_load','max_distance','best_pace','max_duration')),
  value            numeric(12,3) not null,     -- kg | reps | m | s/km | s (conforme record_type)
  load_kg          numeric(7,3),               -- só em max_reps_at_load
  reps             smallint,
  session_id       uuid not null,
  performed_set_id uuid references performed_set(id) on delete set null,
  is_current       boolean not null default true,
  achieved_at      timestamptz not null,
  created_at       timestamptz not null default now(),
  foreign key (session_id, client_id) references workout_session (id, client_id) on delete cascade,
  check ((record_type = 'max_reps_at_load') = (load_kg is not null))
);
-- Um recorde vigente por (aluno, exercício, tipo[, carga])
create unique index one_current_pr on personal_record
  (client_id, exercise_id, record_type, load_kg) nulls not distinct where is_current;
create index on personal_record (client_id, exercise_id, record_type, achieved_at desc);
create index on personal_record (client_id, achieved_at desc) where is_current;
create index on personal_record (session_id);
