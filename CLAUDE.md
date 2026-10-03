# CLAUDE.md

SaaS para personal trainers em **um único app mobile** (iOS e Android), sem painel web: o mesmo app tem o perfil do profissional (alunos, montar treino, central de atenção, treino presencial) e o perfil do aluno (treino do dia, execução offline, evolução). Modelo B2B2C (o profissional paga, o aluno usa grátis). Dados de saúde envolvidos: trate privacidade como requisito, não como detalhe.

## Leia antes de mexer

| Vai trabalhar em | Leia primeiro |
|---|---|
| Qualquer coisa em `backend/` | `docs/BACKEND-PATTERN.md` |
| `apps/mobile`, `packages/` | `docs/FRONTEND-PATTERN.md` |
| Infra, banco, segurança, sync, fotos | `docs/ARQUITETURA.md` |
| Telas, rotas, navegação, estados de tela | `docs/SCREEN-FLOWS.md` |
| "O que fazer agora?" / escopo de uma tarefa | `docs/PLANO.md` |
| Login / projeto do Firebase | `docs/FIREBASE-SETUP.md` |

Os padrões desses documentos são obrigatórios. Se uma regra impedir a tarefa, **não contorne**: explique o conflito e proponha a mudança no documento.

## Mapa do repositório

```
backend/                      Spring Boot (Java 21), monolito modular: accounts, coaching, anamnesis,
                              training, execution, assessment, alerts, billing, audit, shared
backend/src/main/resources/db/migration   Flyway (V1..). Nunca editar migration aplicada.
backend/openapi/openapi.yaml  Contrato da API, gerado no build e commitado
apps/mobile/                  O app (Expo): perfil do aluno e perfil do profissional, offline-first com SQLite
site/                         Página estática mínima: link do convite (universal links), termos e privacidade
packages/api-client/          GERADO pelo Orval a partir do openapi.yaml. Não editar à mão
packages/domain/              Regras puras compartilhadas (TypeScript, sem React)
packages/config/              tsconfig, eslint, prettier base
docs/                         Arquitetura, padrões e plano
```

## Comandos

> Mantenha esta seção atualizada quando um comando mudar.

```bash
cp .env.example .env                       # uma vez; senhas só locais (o .env nunca é commitado)
node backend/scripts/generate-local-kek.mjs >> .env   # uma vez: chave mestra local da criptografia de campo
docker compose up -d                       # Postgres 16 (15432 no host), PgBouncer (6432), S3 local/RustFS (9000/9001)

# backend (precisa de Docker para Testcontainers)
cd backend && ./mvnw verify                # build + todos os testes + ArchUnit + Spotless check + cobertura
./mvnw spotless:apply                      # formatar
./mvnw spring-boot:run -Dspring-boot.run.profiles=local,api      # API
./mvnw spring-boot:run -Dspring-boot.run.profiles=local,worker   # worker (outbox, alertas, push; rode junto com a API)
./mvnw test -Dtest=NomeDoTeste             # um teste
./mvnw test -Dtest=OpenApiContractTest -Dopenapi.update=true   # regrava openapi.yaml (mudança de contrato intencional)

# frontend (na raiz)
pnpm install
pnpm api:generate                          # regenera packages/api-client a partir do openapi.yaml
cp apps/mobile/.env.example apps/mobile/.env   # uma vez; URL da API para o app
pnpm --filter mobile start                 # app (Expo)
pnpm typecheck && pnpm lint && pnpm test   # o mesmo que o CI roda
pnpm --filter @moveup/site build:preview   # site estático (convite, responsável) em site/dist; produção: ver site/README.md
pnpm --filter mobile body-map:generate     # regenera o mapa muscular depois de mudar apps/mobile/assets/body/*.svg
```

## Regras inegociáveis

1. **Camadas:** `domain` não importa Spring, jOOQ, Jackson nem Jakarta. `application` não importa `infrastructure`. Um módulo não acessa `domain`, `infrastructure` nem tabelas de outro módulo; só o pacote `api` dele ou eventos.
2. **Domínio rico:** regra de negócio fica na entidade/value object, com método de nome de negócio. Sem setters públicos.
3. **Erros** sempre em `ProblemDetail` (RFC 9457) pelo handler global, com `code` estável. Nunca expor stack trace, SQL ou dado de saúde.
4. **Banco:** toda transação passa pelo `set_config('app.user_id', ?, true)` (já feito pela infraestrutura). Nunca `SET` de sessão, nunca desligar RLS, nunca `BYPASSRLS`. Tabela nova com `client_id` = RLS + FK composta + teste de RLS.
5. **Migrations:** só criar novas (`V<n>__short_description.sql`, em inglês). Nunca alterar uma existente.
6. **Sem chamada externa dentro de transação.** Efeito externo (push, S3, e-mail, pagamento) = evento no outbox.
7. **Dado de saúde** (anamnese, dor, medidas, fotos, comentários) nunca em log, evento, push, e-mail, Sentry, PostHog ou mensagem de erro.
8. **Frontend:** TypeScript strict, sem `any` e sem `!`. UI não importa `data/`. Todo dado externo validado com Zod na borda. Estado de servidor com TanStack Query.
9. **Offline:** registros de execução têm id UUIDv7 gerado no app e `clientUpdatedAt`; o sync é idempotente. Não quebre isso.
10. **Contrato:** mudou a API → `openapi.yaml` regenerado e `pnpm api:generate` no mesmo PR.

## Como trabalhar

- Tarefa que mexe em mais de 3 arquivos ou em mais de um módulo: **apresente um plano curto antes de codar**.
- Comece pelo domínio e pelo teste. Bug corrigido vem com teste que o reproduz.
- Antes de dizer que terminou: rode `./mvnw verify` e/ou `pnpm typecheck && pnpm lint && pnpm test` e informe o resultado real. Não diga que passou se não rodou.
- Mudanças pequenas e focadas; não refatore o que não foi pedido (sugira no final).
- Ao concluir um item do `docs/PLANO.md`, marque `[x]` somente se os critérios de "Pronto quando" da fase forem atendidos.
- Se a tarefa depender de uma decisão listada como aberta no `PLANO.md`, **pergunte** em vez de decidir.
- Nova dependência (Maven ou npm): justifique antes de adicionar.

## Nunca

- Rodar comandos contra produção ou staging, ou usar credenciais reais.
- Commitar segredo, `.env` ou dado real de aluno (use dados fictícios nos testes e seeds).
- Editar `packages/api-client` à mão ou arquivos gerados do jOOQ.
- `git push --force` em `main`, ou reescrever histórico compartilhado.
- Desativar teste, regra do ESLint, ArchUnit ou checagem do CI para "fazer passar".

## Idioma e convenções

- **Inglês em tudo que é estrutura:** pastas, nomes de arquivo, código, tabelas, colunas, módulos e features (`features/training`, `features/execution`). Pastas e arquivos em kebab-case; componentes React em PascalCase.
- Textos para o usuário, documentação e descrição de PR: **português (Brasil)**.
- Commits: Conventional Commits com escopo do módulo/feature, ex. `feat(execution): registra série offline`.

## Glossário

| Produto (pt) | Código (en) |
|---|---|
| Personal, profissional | `professional` |
| Aluno | `client` |
| Vínculo | `coaching_link` |
| Convite | `invite` |
| Treino / versão | `workout` / `workout_version` |
| Bloco / método | `workout_block` / `method` |
| Sessão (treino executado) | `workout_session` |
| Série planejada / realizada | `prescribed_set` / `performed_set` |
| Anamnese / restrição | `anamnesis` / `health_restriction` |
| Avaliação / foto de evolução | `assessment` / `progress_photo` |
| Recorde pessoal | `personal_record` |
| Central de atenção | `alert` |
