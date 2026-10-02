#!/bin/sh
# Roda uma única vez, na criação do volume do Postgres local.
# Espelha os papéis da ARQUITETURA.md (seção 5):
#   moveup_owner → dono das tabelas; usado só pelo Flyway (pipeline / perfil local)
#   app_api, app_worker, app_report → usados pela aplicação; sem BYPASSRLS, não são donos
# A V12 cria esses papéis como NOLOGIN se não existirem; aqui eles já nascem com LOGIN
# e senha local, então o "if not exists" da V12 não faz nada.
set -eu

psql -v ON_ERROR_STOP=1 --username postgres --dbname postgres \
  -v owner_pw="$DB_OWNER_PASSWORD" \
  -v api_pw="$DB_API_PASSWORD" \
  -v worker_pw="$DB_WORKER_PASSWORD" \
  -v report_pw="$DB_REPORT_PASSWORD" <<'SQL'
create role moveup_owner login password :'owner_pw';
create role app_api    login password :'api_pw'    nobypassrls;
create role app_worker login password :'worker_pw' nobypassrls;
create role app_report login password :'report_pw' nobypassrls;
create database moveup owner moveup_owner;
SQL
