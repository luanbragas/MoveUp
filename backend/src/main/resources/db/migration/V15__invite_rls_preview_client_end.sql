-- =====================================================================
-- V15 — Convite: RLS, prévia antes do aceite e encerramento pelo aluno
--
-- 1. invite ganha RLS: o código é segredo (quem tem o código entra no
--    vínculo). Só o profissional do vínculo enxerga os convites dele; o
--    aluno chega ao convite apenas pelas funções security definer
--    (accept_invite da V12 e invite_preview abaixo).
-- 2. invite_preview(code): "Ana Souza quer ser seu personal" antes do
--    aceite (SCREEN-FLOWS 1.2). Devolve só nome do profissional, nome do
--    negócio e validade; nada do aluno.
-- 3. end_link_as_client(link): o aluno encerra o próprio vínculo
--    (SCREEN-FLOWS 1.2, "Encerre o vínculo atual em Perfil"). A policy de
--    update de coaching_link continua só do profissional.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. RLS do invite
-- ---------------------------------------------------------------------
alter table invite enable row level security;
-- ENABLE sem FORCE (como client e coaching_link na V12): as funções security definer
-- (accept_invite, invite_preview) rodam como dono e precisam enxergar o convite.

create policy invite_professional on invite to app_api
  using (coaching_link_id in (select id from coaching_link
                               where professional_id = app_current_user()))
  with check (coaching_link_id in (select id from coaching_link
                                    where professional_id = app_current_user()));
create policy invite_worker on invite to app_worker using (true) with check (true);
-- app_report: sem policy (o código do convite é segredo)

-- ---------------------------------------------------------------------
-- 2. Prévia do convite
-- ---------------------------------------------------------------------
create function invite_preview(p_code text)
returns table (professional_name text, organization_name text, expires_at timestamptz)
language sql stable security definer set search_path = public as $$
  select u.name, o.name, i.expires_at
    from invite i
    join coaching_link l on l.id = i.coaching_link_id
    join app_user u      on u.id = l.professional_id
    join organization o  on o.id = l.organization_id
   where i.code = p_code
     and app_current_user() is not null           -- só com usuário logado
     and i.accepted_at is null
     and i.revoked_at is null
     and i.expires_at > now()
     and l.status = 'pending'
$$;

revoke execute on function invite_preview(text) from public;
grant execute on function invite_preview(text) to app_api;

-- ---------------------------------------------------------------------
-- 3. Aluno encerra o próprio vínculo
-- ---------------------------------------------------------------------
create function end_link_as_client(p_link uuid) returns void
language plpgsql security definer set search_path = public as $$
declare
  v_client uuid := own_client_id();
begin
  if v_client is null then
    raise exception 'usuário sem cadastro de aluno' using errcode = '28000';
  end if;
  update coaching_link
     set status = 'ended', ended_at = now()
   where id = p_link
     and client_id = v_client
     and status in ('pending', 'active', 'inactive');
  if not found then
    -- inexistente, de outro aluno ou já encerrado: não revela qual
    raise exception 'vínculo não encontrado' using errcode = '28000';
  end if;

  insert into audit_log (actor_id, entity, entity_id, client_id, action)
  values (app_current_user(), 'coaching_link', p_link, v_client, 'link_ended');
end $$;

revoke execute on function end_link_as_client(uuid) from public;
grant execute on function end_link_as_client(uuid) to app_api;
