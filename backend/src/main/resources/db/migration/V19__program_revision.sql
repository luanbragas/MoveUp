-- =====================================================================
-- V19 — Revisão do programa (concorrência otimista, como no treino)
--
-- O personal edita nome, datas e agenda do programa num só "Salvar":
-- GET devolve ETag "r<revision>", PUT/DELETE exigem If-Match.
-- Um programa ativo por vínculo: criar outro arquiva o anterior (na API).
-- =====================================================================

alter table program add column revision int not null default 1 check (revision >= 1);

create index program_link_active_idx on program (coaching_link_id)
  where status = 'active' and deleted_at is null;
