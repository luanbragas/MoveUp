-- =====================================================================
-- V18 — Revisão do treino (concorrência otimista na edição)
--
-- Cada "Salvar" no editor incrementa revision; o GET devolve ETag com ela
-- e o PUT exige If-Match igual (BACKEND-PATTERN: 412 version-mismatch).
-- Não dá para usar o id da versão: sem sessão registrada, a versão atual
-- é substituída no lugar e o id não muda.
-- =====================================================================

alter table workout add column revision int not null default 1 check (revision >= 1);

-- Listar modelos da organização e treinos de um programa sem arquivados.
create index workout_org_templates_idx on workout (organization_id, updated_at desc)
  where is_template and status <> 'archived';
