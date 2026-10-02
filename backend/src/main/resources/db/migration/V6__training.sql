-- =====================================================================
-- V6 — Treinos (o planejado)
-- =====================================================================

create table program (
  id                uuid primary key default uuid_generate_v7(),
  coaching_link_id  uuid not null,
  client_id         uuid not null references client(id),
  name              text not null,
  goal              text,
  starts_on         date,
  ends_on           date,
  schedule_mode     text not null check (schedule_mode in ('fixed_days','sequence')),
  weekly_target     smallint check (weekly_target between 1 and 14),   -- modo sequência
  status            text not null default 'active' check (status in ('active','archived')),
  created_at        timestamptz not null default now(),
  updated_at        timestamptz not null default now(),
  unique (id, client_id),
  foreign key (coaching_link_id, client_id) references coaching_link (id, client_id) on update cascade,
  check (ends_on is null or starts_on is null or ends_on >= starts_on),
  check (schedule_mode <> 'sequence' or weekly_target is not null)
);
create index on program (client_id, status);
create index on program (coaching_link_id);

create table workout (
  id                  uuid primary key default uuid_generate_v7(),
  organization_id     uuid not null references organization(id),
  program_id          uuid references program(id),    -- nulo em template
  is_template         boolean not null default false,
  source_template_id  uuid references workout(id),    -- de qual template veio
  name                text not null,                   -- "Treino A"
  modality            text,
  sequence_position   smallint check (sequence_position >= 1),  -- ordem A/B/C no modo sequência
  current_version_id  uuid,                            -- FK composta adicionada abaixo
  status              text not null default 'active' check (status in ('active','inactive','archived')),
  created_at          timestamptz not null default now(),
  updated_at          timestamptz not null default now(),
  unique (id, program_id),
  unique (program_id, sequence_position),             -- sem dois "Treino B" na sequência
  check (is_template = (program_id is null)),
  check (source_template_id is null or not is_template)
);
create index on workout (program_id);
create index on workout (organization_id) where is_template;

-- Modo dias fixos: Treino A na segunda e quinta.
-- FK composta: o treino precisa pertencer ao mesmo programa.
create table program_schedule (
  program_id  uuid not null references program(id),
  workout_id  uuid not null,
  weekday     smallint not null check (weekday between 0 and 6),   -- 0 = domingo
  primary key (program_id, workout_id, weekday),
  foreign key (workout_id, program_id) references workout (id, program_id)
);

create table workout_version (
  id                 uuid primary key default uuid_generate_v7(),
  workout_id         uuid not null references workout(id),
  version_number     int not null,
  goal               text,                 -- subtítulo do treino
  estimated_minutes  smallint check (estimated_minutes > 0),
  notes              text,
  created_by         uuid not null references app_user(id),
  created_at         timestamptz not null default now(),
  unique (workout_id, version_number),
  unique (workout_id, id)
);

-- A versão atual tem que ser deste treino. Deferrable: dá para criar
-- treino + versão na mesma transação em qualquer ordem.
alter table workout add constraint workout_current_version_fk
  foreign key (id, current_version_id) references workout_version (workout_id, id)
  deferrable initially deferred;

create table workout_block (
  id                   uuid primary key default uuid_generate_v7(),
  workout_version_id   uuid not null references workout_version(id) on delete cascade,
  position             smallint not null,
  name                 text,
  method               text not null check (method in
                       ('sequential','superset','circuit','hiit','emom','amrap','intervals')),
  preset               text,                -- 'tabata' (= hiit 20/10 x8)
  rounds               smallint check (rounds > 0),
  work_seconds         int check (work_seconds > 0),
  rest_seconds         int check (rest_seconds >= 0),
  rest_between_rounds  int check (rest_between_rounds >= 0),
  duration_seconds     int check (duration_seconds > 0),    -- emom / amrap
  config               jsonb not null default '{}',
  -- config: só o que o app lê para montar a tela e que não vira filtro/agregação.
  --   intervals: {"intervals":[{"work":40,"rest":20}]}
  --   amrap:     {"time_cap":600,"scaling":{"rx":"..","scaled":".."}}
  --   emom:      {"odd_minute":"..","even_minute":".."}
  --   circuit:   {"transition_seconds":15}
  --   áudio:     {"beeps":true,"countdown":3}
  notes                text,
  unique (workout_version_id, position) deferrable initially deferred   -- permite reordenar
);

create table prescribed_exercise (
  id            uuid primary key default uuid_generate_v7(),
  block_id      uuid not null references workout_block(id) on delete cascade,
  exercise_id   uuid not null references exercise(id),
  position      smallint not null,
  rest_seconds  int check (rest_seconds >= 0),
  notes         text,                -- orientação para este aluno
  unique (block_id, position) deferrable initially deferred
);
create index on prescribed_exercise (exercise_id);

-- Uma linha por série: permite pirâmide, drop-set e faixa de reps
create table prescribed_set (
  id                      uuid primary key default uuid_generate_v7(),
  prescribed_exercise_id  uuid not null references prescribed_exercise(id) on delete cascade,
  set_number              smallint not null check (set_number >= 1),
  set_type                set_type_t not null default 'normal',
  reps_min                smallint check (reps_min >= 0),
  reps_max                smallint,        -- 8–12 → min 8, max 12
  load_kg                 numeric(7,3) check (load_kg >= 0),
  duration_seconds        int check (duration_seconds > 0),
  distance_m              int check (distance_m > 0),
  target_rpe              numeric(3,1) check (target_rpe between 0 and 10),
  target_rir              smallint check (target_rir between 0 and 10),
  rest_seconds            int check (rest_seconds >= 0),
  unique (prescribed_exercise_id, set_number) deferrable initially deferred,
  check (reps_max is null or reps_min is null or reps_min <= reps_max)
);
