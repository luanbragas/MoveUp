-- =====================================================================
-- V8 — Avaliação física e fotos
-- =====================================================================

create table assessment (
  id                uuid primary key default uuid_generate_v7(),
  client_id         uuid not null references client(id),
  coaching_link_id  uuid,
  assessed_on       date not null,
  weight_kg         numeric(5,2) check (weight_kg > 0),
  height_cm         numeric(5,1) check (height_cm > 0),
  body_fat_pct      numeric(4,1) check (body_fat_pct between 0 and 100),
  body_fat_method   text check (body_fat_method in ('bioimpedance','skinfold','other')),
  notes             text,
  recorded_by       uuid not null references app_user(id),
  recorded_role     text not null check (recorded_role in ('client','professional')),
  created_at        timestamptz not null default now(),
  updated_at        timestamptz not null default now(),
  unique (id, client_id),
  foreign key (coaching_link_id, client_id) references coaching_link (id, client_id) on update cascade,
  check (body_fat_pct is null or body_fat_method is not null)
);
create index on assessment (client_id, assessed_on desc);

create table measurement_type (
  id               uuid primary key default uuid_generate_v7(),
  organization_id  uuid references organization(id),   -- nulo = padrão do sistema
  code             text not null,                       -- waist, arm, thigh...
  name             text not null,
  unit             text not null default 'cm',
  has_sides        boolean not null default false,
  position         smallint,
  unique nulls not distinct (organization_id, code)
);

create table assessment_measurement (
  assessment_id        uuid not null,
  client_id            uuid not null,
  measurement_type_id  uuid not null references measurement_type(id),
  side                 text not null default 'none' check (side in ('none','left','right')),
  value                numeric(6,1) not null check (value > 0),
  primary key (assessment_id, measurement_type_id, side),
  foreign key (assessment_id, client_id) references assessment (id, client_id) on update cascade on delete cascade
);
-- gráfico de evolução de uma medida
create index on assessment_measurement (client_id, measurement_type_id);

-- Arquivos em bucket privado; o banco guarda só o caminho.
-- Upload só liberado com consent 'photos' ativo (validado na API).
-- deleted_at = some da tela na hora; o worker apaga o arquivo do storage.
create table progress_photo (
  id              uuid primary key default uuid_generate_v7(),
  client_id       uuid not null references client(id),
  assessment_id   uuid,
  pose            text not null check (pose in ('front','side','back','other')),
  storage_path    text not null unique,
  thumbnail_path  text,
  taken_on        date not null,
  uploaded_by     uuid not null references app_user(id),
  status          text not null default 'pending' check (status in ('pending','ready','rejected')),
  deleted_at      timestamptz,
  created_at      timestamptz not null default now(),
  foreign key (assessment_id, client_id) references assessment (id, client_id) on update cascade
);
create index on progress_photo (client_id, taken_on) where deleted_at is null;
create index on progress_photo (assessment_id);
