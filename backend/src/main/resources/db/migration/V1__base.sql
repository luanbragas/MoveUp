-- =====================================================================
-- V1 — Extensões, funções utilitárias e domínios
-- PostgreSQL 16+. Convenções: uuid (v7) como PK, timestamptz em UTC,
-- unidades canônicas (kg, cm, m, segundos). Conversão só na tela.
-- =====================================================================

create extension if not exists pg_trgm;
create extension if not exists unaccent;
create extension if not exists citext;

-- UUIDv7: ordenado no tempo (melhor localidade de índice que o v4).
-- O app pode (e deve, nas tabelas do offline) gerar o próprio UUIDv7;
-- este default cobre o que é criado no servidor.
-- (No PostgreSQL 18 trocar por uuidv7() nativo.)
create function uuid_generate_v7() returns uuid
language sql volatile parallel safe as $$
  select encode(
    set_bit(
      set_bit(
        overlay(uuid_send(gen_random_uuid())
                placing substring(int8send(floor(extract(epoch from clock_timestamp()) * 1000)::bigint) from 3)
                from 1 for 6),
        52, 1),
      53, 1),
    'hex')::uuid
$$;

-- unaccent() não é IMMUTABLE; este wrapper pode ser usado em índice.
create function immutable_unaccent(text) returns text
language sql immutable parallel safe strict as
$$ select public.unaccent('public.unaccent'::regdictionary, $1) $$;

-- Mantém updated_at em todo UPDATE (aplicado às tabelas na V11).
create function set_updated_at() returns trigger
language plpgsql as $$
begin
  new.updated_at := now();
  return new;
end $$;

-- Usuário da requisição, definido pela API em cada transação:
--   SET LOCAL app.user_id = '<uuid>';
create function app_current_user() returns uuid
language sql stable as $$
  select nullif(current_setting('app.user_id', true), '')::uuid
$$;

-- Domínios: listas fechadas usadas em mais de uma tabela ficam num lugar só.
create domain sex_t as text
  check (value in ('female','male','other'));

create domain set_type_t as text
  check (value in ('warmup','normal','drop','rest_pause','failure'));

create domain alert_type_t as text
  check (value in ('pain_reported','new_feedback','inactive','low_adherence','high_effort',
                   'assessment_overdue','no_change','session_edited','clearance_pending'));

create domain body_region_t as text;   -- lista controlada pelo app (ombro_esq, joelho_dir, lombar...)
