-- =====================================================================
-- V13 — Chave de dados por aluno (crypto-shredding) e tombstones do sync
--
-- 1. client_key: uma DEK por aluno, cifrada pela chave mestra do KMS
--    (ARQUITETURA.md 7.3). Apagar a linha torna ilegíveis os campos
--    cifrados do aluno em todo lugar, inclusive backups e PITR.
-- 2. deleted_at nas tabelas que o app baixa no GET /v1/sync, para que a
--    exclusão chegue ao aparelho como tombstone (ARQUITETURA.md 4).
--
-- Observações:
--  * Os GRANTs da V12 ("on all tables") só valeram para as tabelas que
--    existiam naquele momento; tabela nova precisa de GRANT explícito.
--  * O trigger de updated_at da V11 também só foi criado para as tabelas
--    existentes; client_key não tem updated_at (a linha não é editada,
--    só criada e, na exclusão de conta, apagada).
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. client_key
-- ---------------------------------------------------------------------
create table client_key (
  client_id      uuid primary key references client(id),
  encrypted_dek  bytea not null check (octet_length(encrypted_dek) between 16 and 4096),
  kms_key_id     text  not null check (length(kms_key_id) between 1 and 2048),
  created_at     timestamptz not null default now()
);

-- Privilégios mínimos:
--   app_api    → lê a DEK dos alunos que pode ler; cria a DEK quando grava
--                o primeiro campo cifrado. Nunca altera nem apaga.
--   app_worker → acesso total (re-cifrar DEK na rotação, crypto-shredding
--                no job de exclusão de conta).
--   app_report → nada.
revoke all on client_key from public, app_api, app_worker, app_report;
grant select, insert on client_key to app_api;
grant select, insert, update, delete on client_key to app_worker;

alter table client_key enable row level security;
alter table client_key force row level security;

create policy client_key_read on client_key for select to app_api
  using (client_id in (select readable_client_ids()));
create policy client_key_insert on client_key for insert to app_api
  with check (client_id in (select writable_client_ids()));
create policy client_key_worker on client_key to app_worker
  using (true) with check (true);
-- app_report: sem GRANT e sem policy.

-- ---------------------------------------------------------------------
-- 2. Tombstones para o sync (workout, program, health_restriction)
--    "Excluir" passa a ser preencher deleted_at; o GET /v1/sync devolve
--    a linha com deleted_at e o app remove localmente.
-- ---------------------------------------------------------------------
alter table program            add column deleted_at timestamptz;
alter table workout            add column deleted_at timestamptz;
alter table health_restriction add column deleted_at timestamptz;

-- Consulta do recebimento: "o que mudou deste aluno desde o cursor − 2 min".
create index program_client_updated_idx
  on program (client_id, updated_at);
create index workout_program_updated_idx
  on workout (program_id, updated_at) where program_id is not null;
create index health_restriction_client_updated_idx
  on health_restriction (client_id, updated_at);
