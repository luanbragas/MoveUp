-- =====================================================================
-- V21 — Worker: fila de push e jobs recorrentes (Fase 4)
-- =====================================================================

-- Push ao profissional, gerado na mesma transação do alerta. Texto sempre neutro (sem nome de aluno
-- nem dado de saúde: aparece na tela bloqueada). O worker marca 'sending' e confirma antes de
-- chamar o Expo: se cair no meio, a mensagem não é reenviada (nunca duplica).
create table push_message (
  id               uuid primary key default uuid_generate_v7(),
  user_id          uuid not null references app_user(id) on delete cascade,
  dedupe_key       text not null unique,      -- ex.: alert:<alert_id>
  title            text not null,
  body             text not null,
  data             jsonb not null default '{}',   -- só ids (ex.: alertId) para o app abrir a tela
  status           text not null default 'pending'
                   check (status in ('pending','sending','sent','failed')),
  attempts         int not null default 0,
  next_attempt_at  timestamptz not null default now(),
  sending_since    timestamptz,
  sent_at          timestamptz,
  last_error       text,
  created_at       timestamptz not null default now(),
  check ((status = 'sending') = (sending_since is not null))
);
create index push_message_pending on push_message (next_attempt_at) where status = 'pending';
create index push_message_sending on push_message (sending_since) where status = 'sending';
grant select, insert, update, delete on push_message to app_worker;

-- db-scheduler (jobs recorrentes do worker: alertas diários, limpeza). Uma linha por tarefa;
-- a trava é a própria linha, então só uma instância do worker roda cada execução.
create table scheduled_tasks (
  task_name             text not null,
  task_instance         text not null,
  task_data             bytea,
  execution_time        timestamptz not null,
  picked                boolean not null,
  picked_by             text,
  last_success          timestamptz,
  last_failure          timestamptz,
  consecutive_failures  int,
  last_heartbeat        timestamptz,
  version               bigint not null,
  priority              smallint,
  primary key (task_name, task_instance)
);
create index scheduled_tasks_execution_time on scheduled_tasks (execution_time);
create index scheduled_tasks_last_heartbeat on scheduled_tasks (last_heartbeat);
create index scheduled_tasks_priority on scheduled_tasks (priority desc, execution_time asc);
grant select, insert, update, delete on scheduled_tasks to app_worker;

-- O app registra o token do aparelho (push_device) e lê/ajusta as configurações de alerta: as duas
-- tabelas não têm dado de saúde; a API filtra pelo usuário autenticado.
create index alert_setting_professional on alert_setting (professional_id);
