-- =====================================================================
-- V12 — Papéis, privilégios, RLS e aceite de convite
--
-- RLS = defesa em profundidade. A API (Spring) faz a autorização principal
-- e, em cada transação, executa:  SET LOCAL app.user_id = '<uuid>';
-- (SET LOCAL funciona com PgBouncer em modo transaction.)
-- Sem app.user_id definido, as policies devolvem zero linhas (falha fechada).
--
-- Papéis:
--   app_api    → API. Não é dono das tabelas, sem BYPASSRLS.
--   app_worker → worker do outbox. Policies próprias liberam acesso total
--                (roda sem usuário: calcula PRs, gera alertas, anonimiza).
--   app_report → somente leitura; não enxerga tabelas de saúde.
-- Senhas/LOGIN são definidos fora das migrations (gerenciador de segredos).
-- =====================================================================

do $$
begin
  if not exists (select 1 from pg_roles where rolname = 'app_api')    then create role app_api    nologin; end if;
  if not exists (select 1 from pg_roles where rolname = 'app_worker') then create role app_worker nologin; end if;
  if not exists (select 1 from pg_roles where rolname = 'app_report') then create role app_report nologin; end if;
end $$;

grant usage on schema public to app_api, app_worker, app_report;
grant select, insert, update, delete on all tables in schema public to app_api, app_worker;
grant select on all tables in schema public to app_report;
grant usage on all sequences in schema public to app_api, app_worker;

-- audit_log é append-only (pai e cada partição)
do $$
declare t regclass;
begin
  for t in select 'audit_log'::regclass
           union all
           select inhrelid::regclass from pg_inherits where inhparent = 'audit_log'::regclass
  loop
    execute format('revoke update, delete, truncate on %s from app_api, app_worker, app_report', t);
  end loop;
end $$;

-- ---------------------------------------------------------------------
-- Conjuntos de clientes acessíveis. Avaliados UMA vez por consulta
-- quando usados como  client_id in (select f())  (subplano), em vez de
-- dois EXISTS por linha.
-- security definer: rodam como dono, que não passa pelo RLS de client e
-- coaching_link (essas duas tabelas usam ENABLE sem FORCE de propósito,
-- senão as policies delas chamariam estas funções em recursão).
-- ---------------------------------------------------------------------

create function own_client_id() returns uuid
language sql stable security definer set search_path = public as $$
  select id from client where user_id = app_current_user()
$$;

-- Leitura: o próprio aluno + vínculos pendentes, ativos e inativos
-- (inativo = o personal ainda consulta o histórico para reativar).
create function readable_client_ids() returns setof uuid
language sql stable security definer set search_path = public as $$
  select id from client where user_id = app_current_user()
  union
  select client_id from coaching_link
   where professional_id = app_current_user()
     and status in ('pending','active','inactive')
$$;

-- Escrita: o próprio aluno + vínculos pendentes e ativos.
create function writable_client_ids() returns setof uuid
language sql stable security definer set search_path = public as $$
  select id from client where user_id = app_current_user()
  union
  select client_id from coaching_link
   where professional_id = app_current_user()
     and status in ('pending','active')
$$;

create function my_organization_ids() returns setof uuid
language sql stable security definer set search_path = public as $$
  select organization_id from organization_member where user_id = app_current_user()
$$;

revoke execute on function own_client_id(), readable_client_ids(),
                           writable_client_ids(), my_organization_ids() from public;
grant execute on function own_client_id(), readable_client_ids(),
                          writable_client_ids(), my_organization_ids() to app_api, app_worker;

-- ---------------------------------------------------------------------
-- Tabelas com dado do aluno (todas têm client_id protegido por FK composta)
-- ---------------------------------------------------------------------
do $$
declare t text;
begin
  foreach t in array array[
    'anamnesis','health_restriction','program',
    'workout_session','performed_exercise','performed_set','block_result',
    'session_feedback','pain_report','personal_record',
    'assessment','assessment_measurement','progress_photo']
  loop
    execute format('alter table %I enable row level security', t);
    execute format('alter table %I force row level security', t);

    execute format($p$create policy %I on %I for select to app_api
                      using (client_id in (select readable_client_ids()))$p$, t || '_read', t);
    execute format($p$create policy %I on %I for insert to app_api
                      with check (client_id in (select writable_client_ids()))$p$, t || '_insert', t);
    execute format($p$create policy %I on %I for update to app_api
                      using (client_id in (select writable_client_ids()))
                      with check (client_id in (select writable_client_ids()))$p$, t || '_update', t);
    execute format($p$create policy %I on %I for delete to app_api
                      using (client_id in (select writable_client_ids()))$p$, t || '_delete', t);

    execute format($p$create policy %I on %I to app_worker
                      using (true) with check (true)$p$, t || '_worker', t);
    -- app_report: nenhuma policy = não lê dado de saúde
  end loop;
end $$;

-- ---------------------------------------------------------------------
-- client e coaching_link: ENABLE sem FORCE (ver nota acima)
-- ---------------------------------------------------------------------
alter table client enable row level security;
create policy client_read   on client for select to app_api
  using (id in (select readable_client_ids()) or created_by = app_current_user());
create policy client_insert on client for insert to app_api
  with check (created_by = app_current_user());
create policy client_update on client for update to app_api
  using (id in (select writable_client_ids()))
  with check (id in (select writable_client_ids()));
create policy client_worker on client to app_worker using (true) with check (true);

alter table coaching_link enable row level security;
create policy link_read on coaching_link for select to app_api
  using (professional_id = app_current_user() or client_id = (select own_client_id()));
create policy link_insert on coaching_link for insert to app_api
  with check (professional_id = app_current_user()
              and organization_id in (select my_organization_ids()));
create policy link_update on coaching_link for update to app_api
  using (professional_id = app_current_user())
  with check (professional_id = app_current_user());
create policy link_worker on coaching_link to app_worker using (true) with check (true);

-- ---------------------------------------------------------------------
-- Só o profissional (o aluno nunca vê)
-- ---------------------------------------------------------------------
alter table client_note enable row level security;
alter table client_note force row level security;
create policy client_note_author on client_note to app_api
  using (author_id = app_current_user())
  with check (author_id = app_current_user());
create policy client_note_worker on client_note to app_worker using (true) with check (true);

alter table alert enable row level security;
alter table alert force row level security;
create policy alert_professional on alert to app_api
  using (professional_id = app_current_user())
  with check (professional_id = app_current_user());
create policy alert_worker on alert to app_worker using (true) with check (true);

-- Treinos (workout, workout_version, workout_block, prescribed_*): não são
-- dado de saúde; a API autoriza por organização/programa. Se quiser RLS
-- nelas, usar organization_id in (select my_organization_ids()) em workout
-- e EXISTS até workout nas filhas.

-- ---------------------------------------------------------------------
-- Aceite de convite. Roda como dono porque o aluno ainda não "possui" o
-- cadastro antes de aceitar. Também resolve a corrida do limite do plano:
-- trava a assinatura (FOR UPDATE), então dois aceites simultâneos da mesma
-- organização são serializados antes da contagem.
-- Use a mesma trava + contagem ao REATIVAR um aluno inativo (na API).
-- ---------------------------------------------------------------------
create function accept_invite(p_code text) returns uuid
language plpgsql security definer set search_path = public as $$
declare
  v_user      uuid := app_current_user();
  v_invite    invite;
  v_link      coaching_link;
  v_owner     uuid;
  v_existing  uuid;
  v_client    uuid;
  v_limit     int;
  v_active    int;
begin
  if v_user is null then
    raise exception 'app.user_id não definido' using errcode = '28000';
  end if;

  select * into v_invite from invite where code = p_code for update;
  if not found or v_invite.revoked_at is not null
     or v_invite.accepted_at is not null or v_invite.expires_at < now() then
    raise exception 'convite inválido ou expirado' using errcode = 'P0002';
  end if;

  select * into v_link from coaching_link where id = v_invite.coaching_link_id for update;
  if v_link.status <> 'pending' then
    raise exception 'vínculo não está pendente' using errcode = 'P0001';
  end if;

  select p.max_active_clients into v_limit
    from subscription s join plan p on p.id = s.plan_id
   where s.organization_id = v_link.organization_id
     and s.status in ('trialing','active','past_due')
     for update of s;
  if not found then
    raise exception 'organização sem assinatura ativa' using errcode = 'P0001';
  end if;

  select count(*) into v_active from coaching_link
   where organization_id = v_link.organization_id and status = 'active';
  if v_active >= v_limit then
    raise exception 'limite de alunos do plano atingido' using errcode = 'P0003';
  end if;

  select id into v_existing from client where user_id = v_user;
  v_client := coalesce(v_existing, v_link.client_id);

  if v_existing is null then
    select user_id into v_owner from client where id = v_link.client_id for update;
    if v_owner is not null and v_owner <> v_user then
      raise exception 'convite pertence a outro usuário' using errcode = '28000';
    end if;
    update client set user_id = v_user where id = v_link.client_id;
  elsif v_existing <> v_link.client_id then
    -- O aluno já tem cadastro (ex.: personal anterior). O vínculo passa a
    -- apontar para ele; as FKs compostas levam junto o que o personal já
    -- tinha lançado no pré-cadastro (programa, anamnese, avaliação).
    perform set_config('app.system_change', 'on', true);
    update coaching_link set client_id = v_existing where id = v_link.id;
    perform set_config('app.system_change', '', true);
    begin
      delete from client where id = v_link.client_id;   -- pré-cadastro vazio
    exception when foreign_key_violation then
      null;  -- ainda referenciado por outro vínculo: fica
    end;
  end if;

  begin
    update coaching_link set status = 'active', started_at = now() where id = v_link.id;
  exception when unique_violation then
    raise exception 'aluno já tem um vínculo ativo com outro profissional' using errcode = 'P0004';
  end;

  update invite set accepted_at = now(), accepted_by = v_user where id = v_invite.id;

  insert into audit_log (actor_id, entity, entity_id, client_id, action)
  values (v_user, 'coaching_link', v_link.id, v_client, 'link_created');

  return v_link.id;
end $$;

revoke execute on function accept_invite(text) from public;
grant execute on function accept_invite(text) to app_api;
