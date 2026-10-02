-- =====================================================================
-- V14 — Consentimento do responsável (aluno menor de idade) e plano de teste
--
-- Decisões de 02/10/2026 (PLANO.md):
--  * Menores de 18 anos podem usar o app com consentimento de um dos pais
--    ou do responsável legal (LGPD, art. 14). O MVP registra a declaração
--    feita no app; verified_at fica para a confirmação por e-mail ao
--    responsável (pendente: provedor de e-mail e revisão jurídica, Fase 9).
--  * Toda organização nova começa num plano de teste. O plano 'trial' é
--    PROVISÓRIO: preço e limites definitivos são decisão aberta da Fase 8.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. guardian_consent
-- ---------------------------------------------------------------------
create table guardian_consent (
  id              uuid primary key default uuid_generate_v7(),
  user_id         uuid not null references app_user(id),   -- o menor
  guardian_name   text not null check (length(guardian_name) between 2 and 200),
  guardian_email  citext not null,
  relationship    text not null
                  check (relationship in ('mother','father','legal_guardian','other')),
  doc_version     text not null,
  granted_at      timestamptz not null default now(),
  ip              inet,
  user_agent      text,
  verified_at     timestamptz,                             -- confirmação pelo responsável (futuro)
  revoked_at      timestamptz,
  created_at      timestamptz not null default now(),
  check (revoked_at is null or revoked_at >= granted_at),
  check (verified_at is null or verified_at >= granted_at)
);
-- No máximo um consentimento vigente por menor (revogar antes de registrar outro).
create unique index one_active_guardian_consent on guardian_consent (user_id)
  where revoked_at is null;

-- Tabela nova: GRANTs explícitos (os da V12 só valeram para as tabelas da época).
--   app_api    → o próprio menor registra e revoga; nunca apaga (histórico LGPD).
--   app_worker → acesso total (exclusão de conta).
--   app_report → nada (dado pessoal de terceiro).
revoke all on guardian_consent from public, app_api, app_worker, app_report;
grant select, insert, update on guardian_consent to app_api;
grant select, insert, update, delete on guardian_consent to app_worker;

alter table guardian_consent enable row level security;
alter table guardian_consent force row level security;

-- Só o próprio usuário: nem o personal vê os dados do responsável.
create policy guardian_consent_owner on guardian_consent to app_api
  using (user_id = app_current_user())
  with check (user_id = app_current_user());
create policy guardian_consent_worker on guardian_consent to app_worker
  using (true) with check (true);

-- ---------------------------------------------------------------------
-- 2. Plano de teste (provisório; ver cabeçalho)
-- ---------------------------------------------------------------------
insert into plan (code, name, max_active_clients, price_cents, billing_interval)
values ('trial', 'Teste grátis', 10, 0, 'month');

-- ---------------------------------------------------------------------
-- 3. Papel da conta (MVP: uma conta tem um papel; SCREEN-FLOWS 0.1).
--    Nulo = cadastro ainda não escolheu o perfil.
-- ---------------------------------------------------------------------
alter table app_user add column role text check (role in ('professional','client'));
