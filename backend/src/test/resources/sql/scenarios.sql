\set ON_ERROR_STOP 0
-- seed (superusuário)
insert into app_user(id,name,email) values
 ('00000000-0000-0000-0000-0000000000a1','Pro A','a@x.com'),
 ('00000000-0000-0000-0000-0000000000b1','Pro B','b@x.com'),
 ('00000000-0000-0000-0000-0000000000c1','Aluno','c@x.com');
insert into organization(id,name,owner_user_id) values
 ('00000000-0000-0000-0000-00000000aa01','Org A','00000000-0000-0000-0000-0000000000a1'),
 ('00000000-0000-0000-0000-00000000bb01','Org B','00000000-0000-0000-0000-0000000000b1');
insert into organization_member values
 ('00000000-0000-0000-0000-00000000aa01','00000000-0000-0000-0000-0000000000a1','owner'),
 ('00000000-0000-0000-0000-00000000bb01','00000000-0000-0000-0000-0000000000b1','owner');
insert into plan(id,code,name,max_active_clients,price_cents,billing_interval) values
 ('00000000-0000-0000-0000-0000000000f1','tiny','Tiny',1,0,'month');
insert into subscription(organization_id,plan_id,status,trial_ends_at) values
 ('00000000-0000-0000-0000-00000000aa01','00000000-0000-0000-0000-0000000000f1','trialing',now()+interval '14 days'),
 ('00000000-0000-0000-0000-00000000bb01','00000000-0000-0000-0000-0000000000f1','trialing',now()+interval '14 days');

\echo '--- 1. uuid v7'
select substr(uuid_generate_v7()::text,15,1) as versao;

\echo '--- 2. Pro A pré-cadastra aluno e cria convite (como app_api)'
begin; set local role app_api; set local app.user_id='00000000-0000-0000-0000-0000000000a1';
insert into client(id,name,created_by) values ('00000000-0000-0000-0000-0000000c0001','Aluno','00000000-0000-0000-0000-0000000000a1');
insert into coaching_link(id,organization_id,professional_id,client_id,status) values
 ('00000000-0000-0000-0000-000000000d01','00000000-0000-0000-0000-00000000aa01','00000000-0000-0000-0000-0000000000a1','00000000-0000-0000-0000-0000000c0001','pending');
commit;
insert into invite(coaching_link_id,code,expires_at) values ('00000000-0000-0000-0000-000000000d01','ABC123',now()+interval '7 days');

\echo '--- 3. aluno aceita o convite'
begin; set local role app_api; set local app.user_id='00000000-0000-0000-0000-0000000000c1';
select accept_invite('ABC123') is not null as aceito;
commit;
select status, client_id from coaching_link;

\echo '--- 4. aluno registra sessão offline (id do app)'
begin; set local role app_api; set local app.user_id='00000000-0000-0000-0000-0000000000c1';
insert into workout_session(id,client_id,coaching_link_id,status,started_at,performed_by,performed_by_user,client_updated_at)
values ('00000000-0000-0000-0000-00000000e001','00000000-0000-0000-0000-0000000c0001','00000000-0000-0000-0000-000000000d01','in_progress',now(),'client','00000000-0000-0000-0000-0000000000c1',now());
commit;

\echo '--- 5. Pro A vê a sessão (esperado 1), Pro B não vê (esperado 0), sem usuário (0)'
begin; set local role app_api; set local app.user_id='00000000-0000-0000-0000-0000000000a1'; select count(*) from workout_session; commit;
begin; set local role app_api; set local app.user_id='00000000-0000-0000-0000-0000000000b1'; select count(*) from workout_session; commit;
begin; set local role app_api; select count(*) from workout_session; commit;

\echo '--- 6. Pro B tenta inserir sessão no aluno de A (esperado: erro RLS)'
begin; set local role app_api; set local app.user_id='00000000-0000-0000-0000-0000000000b1';
insert into workout_session(id,client_id,coaching_link_id,status,started_at,performed_by,performed_by_user,client_updated_at)
values (uuid_generate_v7(),'00000000-0000-0000-0000-0000000c0001','00000000-0000-0000-0000-000000000d01','in_progress',now(),'professional','00000000-0000-0000-0000-0000000000b1',now());
rollback;

\echo '--- 7. FK composta: exercício com client_id diferente da sessão (esperado: erro FK)'
insert into client(id,name) values ('00000000-0000-0000-0000-0000000c0002','Outro');
insert into exercise(id,name,modality,tracking_type) values ('00000000-0000-0000-0000-0000000ee001','Supino reto','strength','reps_load');
insert into performed_exercise(id,session_id,client_id,exercise_id,position,status,client_updated_at)
values (uuid_generate_v7(),'00000000-0000-0000-0000-00000000e001','00000000-0000-0000-0000-0000000c0002','00000000-0000-0000-0000-0000000ee001',1,'done',now());

\echo '--- 8. worker grava PR sem usuário (esperado: ok)'
begin; set local role app_worker;
insert into personal_record(client_id,exercise_id,record_type,value,session_id,achieved_at)
values ('00000000-0000-0000-0000-0000000c0001','00000000-0000-0000-0000-0000000ee001','max_load',32,'00000000-0000-0000-0000-00000000e001',now());
commit;
select count(*) as prs from personal_record;

\echo '--- 9. medida do sistema duplicada (esperado: erro unique)'
insert into measurement_type(code,name) values ('waist','Cintura');
insert into measurement_type(code,name) values ('waist','Cintura');

\echo '--- 10. exercício duplicado ignorando acento/caixa (esperado: erro unique)'
insert into exercise(name,modality,tracking_type) values ('SUPINO RETO','strength','reps_load');

\echo '--- 11. alerta: mesmo dedupe em profissionais diferentes (esperado: ok, ok, erro)'
insert into alert(organization_id,professional_id,client_id,type,severity,facts,dedupe_key) values
 ('00000000-0000-0000-0000-00000000aa01','00000000-0000-0000-0000-0000000000a1','00000000-0000-0000-0000-0000000c0001','inactive','warning','{}','inactive:c1');
insert into alert(organization_id,professional_id,client_id,type,severity,facts,dedupe_key) values
 ('00000000-0000-0000-0000-00000000bb01','00000000-0000-0000-0000-0000000000b1','00000000-0000-0000-0000-0000000c0001','inactive','warning','{}','inactive:c1');
insert into alert(organization_id,professional_id,client_id,type,severity,facts,dedupe_key) values
 ('00000000-0000-0000-0000-00000000aa01','00000000-0000-0000-0000-0000000000a1','00000000-0000-0000-0000-0000000c0001','inactive','warning','{}','inactive:c1');

\echo '--- 12. limite do plano (max 1): segundo aluno da Org A (esperado: erro limite)'
insert into app_user(id,name,email) values ('00000000-0000-0000-0000-0000000000c2','Aluno 2','c2@x.com');
insert into client(id,name) values ('00000000-0000-0000-0000-0000000c0003','Aluno 2');
insert into coaching_link(id,organization_id,professional_id,client_id,status) values
 ('00000000-0000-0000-0000-000000000d02','00000000-0000-0000-0000-00000000aa01','00000000-0000-0000-0000-0000000000a1','00000000-0000-0000-0000-0000000c0003','pending');
insert into invite(coaching_link_id,code,expires_at) values ('00000000-0000-0000-0000-000000000d02','LIM1',now()+interval '1 day');
begin; set local role app_api; set local app.user_id='00000000-0000-0000-0000-0000000000c2';
select accept_invite('LIM1'); rollback;

\echo '--- 13. troca de personal: aluno encerra com A, aceita B com pré-cadastro (merge)'
update coaching_link set status='ended', ended_at=now() where id='00000000-0000-0000-0000-000000000d01';
insert into client(id,name) values ('00000000-0000-0000-0000-0000000c0009','Aluno (pré-cad. B)');
insert into coaching_link(id,organization_id,professional_id,client_id,status) values
 ('00000000-0000-0000-0000-000000000d09','00000000-0000-0000-0000-00000000bb01','00000000-0000-0000-0000-0000000000b1','00000000-0000-0000-0000-0000000c0009','pending');
insert into program(coaching_link_id,client_id,name,schedule_mode) values
 ('00000000-0000-0000-0000-000000000d09','00000000-0000-0000-0000-0000000c0009','Hipertrofia','fixed_days');
insert into invite(coaching_link_id,code,expires_at) values ('00000000-0000-0000-0000-000000000d09','TROCA',now()+interval '1 day');
begin; set local role app_api; set local app.user_id='00000000-0000-0000-0000-0000000000c1';
select accept_invite('TROCA') is not null as aceito; commit;
select (select client_id from coaching_link where id='00000000-0000-0000-0000-000000000d09') as link_client,
       (select client_id from program) as program_client,
       exists(select 1 from client where id='00000000-0000-0000-0000-0000000c0009') as placeholder_existe;

\echo '--- 14. Pro A (vínculo encerrado) não lê mais; Pro B lê o histórico (esperado 0 e 1)'
begin; set local role app_api; set local app.user_id='00000000-0000-0000-0000-0000000000a1'; select count(*) from workout_session; commit;
begin; set local role app_api; set local app.user_id='00000000-0000-0000-0000-0000000000b1'; select count(*) from workout_session; commit;

\echo '--- 15. anamnese revisada é imutável (esperado: erro)'
insert into anamnesis_template(id,name,version,questions) values ('00000000-0000-0000-0000-00000000a0a1','Padrão',1,'[]');
insert into anamnesis(id,client_id,template_id,version_number,answers,filled_by,reviewed_by,reviewed_at) values
 ('00000000-0000-0000-0000-00000000a0a2','00000000-0000-0000-0000-0000000c0001','00000000-0000-0000-0000-00000000a0a1',1,'{}','00000000-0000-0000-0000-0000000000c1','00000000-0000-0000-0000-0000000000b1',now());
update anamnesis set goal='x';
delete from anamnesis;

\echo '--- 16. updated_at automático'
update workout_session set status='completed', finished_at=now() where id='00000000-0000-0000-0000-00000000e001';
select updated_at > created_at as atualizado from workout_session;

\echo '--- 17. audit_log append-only para app_api (esperado: erro permissão)'
begin; set local role app_api; delete from audit_log; rollback;

\echo '--- 18. current_version de outro treino (esperado: erro FK no commit)'
begin;
insert into workout(id,organization_id,is_template,name) values
 ('00000000-0000-0000-0000-000000000f01','00000000-0000-0000-0000-00000000aa01',true,'T1');
rollback;
begin;
insert into workout(id,organization_id,is_template,name) values ('00000000-0000-0000-0000-000000000f01','00000000-0000-0000-0000-00000000aa01',true,'T1'),('00000000-0000-0000-0000-000000000f02','00000000-0000-0000-0000-00000000aa01',true,'T2');
insert into workout_version(id,workout_id,version_number,created_by) values ('00000000-0000-0000-0000-000000000f11','00000000-0000-0000-0000-000000000f02',1,'00000000-0000-0000-0000-0000000000a1');
update workout set current_version_id='00000000-0000-0000-0000-000000000f11' where id='00000000-0000-0000-0000-000000000f01';
commit;
