-- =====================================================================
-- V5 — Exercícios
-- =====================================================================

create table exercise (
  id                 uuid primary key default uuid_generate_v7(),
  organization_id    uuid references organization(id),   -- nulo = biblioteca base
  name               text not null,
  modality           text not null check (modality in ('strength','cardio','conditioning','complementary')),
  tracking_type      text not null check (tracking_type in ('reps_load','reps_only','time','distance_time')),
  primary_muscle     text,
  secondary_muscles  text[] not null default '{}',
  equipment          text,
  is_unilateral      boolean not null default false,
  instructions       text,           -- como executar o movimento (genérico)
  media_url          text,           -- link externo (YouTube)
  media_path         text,           -- upload próprio no storage
  archived_at        timestamptz,
  created_by         uuid references app_user(id),
  created_at         timestamptz not null default now()
);

-- Sem nomes repetidos na mesma biblioteca (ignorando acento e caixa)
create unique index exercise_unique_name on exercise
  (organization_id, immutable_unaccent(lower(name))) nulls not distinct
  where archived_at is null;

-- Busca por nome: where immutable_unaccent(lower(name)) % immutable_unaccent(lower($1))
create index exercise_name_trgm on exercise
  using gin (immutable_unaccent(lower(name)) gin_trgm_ops);
