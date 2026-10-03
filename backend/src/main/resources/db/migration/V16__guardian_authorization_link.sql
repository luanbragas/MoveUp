-- =====================================================================
-- V16 — Autorização do responsável pelo link (substitui a declaração no app)
--
-- Decisão de 03/10/2026: o menor só indica quem é o responsável; quem
-- autoriza é o próprio responsável, pelo link que recebe no celular dele
-- (o app do menor compartilha o link pelo WhatsApp ou outro app).
--
--  * guardian_consent vira um pedido: granted_at = quando o menor pediu;
--    verified_at = quando o responsável autorizou (só então vale);
--    declined_at = quando recusou. revoked_at continua sendo o fim do
--    pedido (cancelado pelo menor, recusado ou revogado).
--  * O link carrega um segredo de 256 bits; o banco guarda só o SHA-256.
--    Reenviar troca o segredo (o link anterior deixa de valer).
--  * guardian_email deixa de ser obrigatório: o link vai pelo app do
--    menor, então não guardamos telefone nem e-mail de terceiro sem uso
--    (minimização, LGPD art. 6º, III).
--  * O responsável não tem conta: chega ao pedido só pelas funções
--    security definer abaixo, com o hash do segredo. Por isso a tabela
--    passa a ENABLE sem FORCE (mesmo padrão de client, coaching_link e
--    invite): o dono, que executa as funções, enxerga as linhas; app_api
--    continua preso às policies da V14.
-- =====================================================================

alter table guardian_consent alter column guardian_email drop not null;

alter table guardian_consent
  add column token_hash         bytea unique check (octet_length(token_hash) = 32),
  add column token_expires_at   timestamptz,
  add column declined_at        timestamptz,
  add column decided_ip         inet,
  add column decided_user_agent text check (length(decided_user_agent) <= 500),
  add constraint guardian_consent_one_decision check (verified_at is null or declined_at is null),
  add constraint guardian_consent_token_expiry check ((token_hash is null) = (token_expires_at is null));

alter table guardian_consent no force row level security;

-- ---------------------------------------------------------------------
-- Pedido pelo segredo do link. Devolve o mínimo para a página do
-- responsável e para a decisão: primeiro nome do menor, nome e parentesco
-- indicados, versão do texto, validade. Pedido decidido, cancelado ou
-- vencido não aparece (o responsável vê "link inválido ou vencido").
-- ---------------------------------------------------------------------
create function guardian_request_by_token(p_token_hash bytea)
returns table (id uuid, user_id uuid, minor_first_name text, guardian_name text,
               relationship text, doc_version text, granted_at timestamptz,
               token_expires_at timestamptz)
language sql stable security definer set search_path = public as $$
  select g.id, g.user_id, split_part(u.name, ' ', 1), g.guardian_name,
         g.relationship, g.doc_version, g.granted_at, g.token_expires_at
    from guardian_consent g
    join app_user u on u.id = g.user_id
   where g.token_hash = p_token_hash
     and g.verified_at is null
     and g.declined_at is null
     and g.revoked_at is null
     and g.token_expires_at > now()
$$;

-- ---------------------------------------------------------------------
-- Grava a decisão do responsável. A regra (versão do texto, validade) é
-- do domínio; aqui só a trava: o segredo precisa bater e o pedido ainda
-- estar aberto, senão nada muda e a função devolve false. O segredo é
-- apagado: o link vale uma vez. A prova da decisão (quando, IP e app)
-- fica na própria linha.
-- ---------------------------------------------------------------------
create function guardian_request_decide(p_token_hash bytea, p_approved boolean,
                                        p_doc_version text, p_decided_at timestamptz,
                                        p_ip inet, p_user_agent text)
returns boolean
language plpgsql security definer set search_path = public as $$
declare
  v_id uuid;
begin
  select g.id into v_id
    from guardian_consent g
   where g.token_hash = p_token_hash
     and g.verified_at is null
     and g.declined_at is null
     and g.revoked_at is null
     and g.token_expires_at > now()
   for update;
  if not found then
    return false;
  end if;

  update guardian_consent
     set verified_at        = case when p_approved then p_decided_at end,
         declined_at        = case when p_approved then null else p_decided_at end,
         revoked_at         = case when p_approved then null else p_decided_at end,
         doc_version        = p_doc_version,
         decided_ip         = p_ip,
         decided_user_agent = p_user_agent,
         token_hash         = null,
         token_expires_at   = null
   where id = v_id;
  return true;
end $$;

revoke execute on function guardian_request_by_token(bytea) from public;
revoke execute on function guardian_request_decide(bytea, boolean, text, timestamptz, inet, text) from public;
grant execute on function guardian_request_by_token(bytea) to app_api;
grant execute on function guardian_request_decide(bytea, boolean, text, timestamptz, inet, text) to app_api;
