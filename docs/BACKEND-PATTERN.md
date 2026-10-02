# Padrões do Backend

Regras obrigatórias para todo código em `backend/`. Quando uma regra daqui conflitar com conveniência, vale a regra. Quando uma regra precisar mudar, muda **este arquivo primeiro**, no mesmo PR.

**Stack:** Java 21 · Spring Boot 3 · Spring Security (resource server) · jOOQ · Flyway · PostgreSQL 16 · springdoc-openapi · JUnit 5 · AssertJ · Testcontainers · ArchUnit.

> Pacote base: `br.com.moveup` (produto **MoveUp**, decidido em 01/10/2026).

---

## 1. Princípios

1. **O domínio manda.** Regra de negócio mora em entidades e value objects, não em controllers, services "anêmicos" ou SQL.
2. **Dependências apontam para dentro.** `infrastructure → application → domain`. Nunca o contrário.
3. **Framework é detalhe.** `domain` não conhece Spring, jOOQ, Jackson nem HTTP.
4. **Cada módulo é dono dos seus dados.** Nenhum módulo lê ou escreve tabela de outro.
5. **Falhar cedo e explícito.** Objeto inválido não chega a existir; erro vira `ProblemDetail` com `code` estável.
6. **Dado de saúde é radioativo.** Nunca em log, métrica, push, e-mail, mensagem de erro ou analytics.
7. **Teste prova o comportamento**, não a implementação.

---

## 2. Monolito modular

Um único deploy, dividido em módulos com fronteira explícita. Se um módulo um dia virar serviço, a fronteira já existe.

| Módulo | Responsabilidade | Tabelas principais |
|---|---|---|
| `accounts` | Usuário, identidade, organização, consentimento | `app_user`, `auth_identity`, `organization*`, `professional_profile`, `consent` |
| `coaching` | Cliente, vínculo, convite, notas | `client`, `coaching_link`, `invite`, `client_note` |
| `anamnesis` | Anamnese versionada e restrições | `anamnesis*`, `health_restriction` |
| `training` | O planejado: exercícios, programas, treinos versionados | `exercise`, `program*`, `workout*`, `prescribed_*` |
| `execution` | O realizado: sessões, séries, feedback, dor, PRs | `workout_session`, `performed_*`, `block_result`, `session_feedback`, `pain_report`, `personal_record` |
| `assessment` | Avaliação física e fotos | `assessment*`, `measurement_type`, `progress_photo` |
| `alerts` | Central de atenção e push | `alert*`, `push_device` |
| `billing` | Planos, assinatura, webhooks | `plan`, `subscription`, `payment_event` |
| `audit` | Trilha de acesso a dado sensível | `audit_log` |
| `shared` | Kernel compartilhado: ids, tempo, erros base, outbox, segurança | `outbox_event` |

**Como módulos conversam (só destas duas formas):**

- **Chamada síncrona** a uma *porta de entrada pública* do outro módulo (interface em `application/port/in`, exportada pelo pacote `api` do módulo). Ex.: `execution` pergunta a `coaching` se existe vínculo ativo via `CoachingLinkQuery`.
- **Evento de domínio** gravado no outbox na mesma transação e consumido pelo worker. Ex.: `execution` publica `session.finished`; `alerts` reage.

Proibido: importar `domain` ou `infrastructure` de outro módulo, fazer JOIN com tabela de outro módulo, compartilhar entidades. Se precisar de dado de outro módulo para leitura de tela, crie uma **query de leitura** na porta pública dele que devolva um DTO.

As regras são verificadas por ArchUnit (seção 12) e quebram o build.

---

## 3. Estrutura de pacotes (clean architecture + hexagonal)

```
br.com.moveup.<modulo>/
├── api/                         ← o que outros módulos podem usar (re-exporta portas in + DTOs)
├── domain/
│   ├── model/                   ← entidades, agregados, value objects
│   ├── event/                   ← eventos de domínio (records)
│   ├── service/                 ← serviços de domínio (regra que envolve mais de um agregado)
│   └── exception/               ← exceções de domínio com código estável
├── application/
│   ├── port/
│   │   ├── in/                  ← casos de uso (interfaces) + commands/queries/results (records)
│   │   └── out/                 ← o que a aplicação precisa do mundo: repositórios, storage, push, relógio
│   └── usecase/                 ← implementações dos casos de uso
└── infrastructure/
    ├── web/                     ← adapters de entrada: controllers REST, DTOs de request/response, mappers
    ├── persistence/             ← adapters de saída: repositórios jOOQ, mappers registro ↔ domínio
    ├── messaging/               ← handlers de outbox, publicação de eventos
    ├── integration/             ← S3, push, gateway de pagamento, KMS
    └── config/                  ← @Configuration: monta os beans do módulo
```

| Camada | Pode depender de | Não pode depender de |
|---|---|---|
| `domain` | `java.*`, `shared.domain` | Spring, jOOQ, Jackson, Jakarta, qualquer outra camada |
| `application` | `domain`, `shared`, `org.springframework.transaction` (só `@Transactional`) | `infrastructure`, web, jOOQ |
| `infrastructure` | tudo acima + frameworks | `infrastructure` de outro módulo |

**Portas e adaptadores:**

- Porta de entrada (`port/in`): um caso de uso por interface, com nome de intenção de negócio: `FinishSession`, `AcceptInvite`, `ReviseWorkout`.
- Porta de saída (`port/out`): interface pensada pelo lado da aplicação, não do banco: `WorkoutRepository`, `PhotoStorage`, `PushSender`, `Clock`, `FieldCipher`.
- Adaptador: implementa uma porta e fica em `infrastructure`. Trocar S3 por R2 = escrever outro adaptador de `PhotoStorage`.

**Composição sem acoplar a aplicação ao Spring:** casos de uso são classes Java puras (sem `@Service`). Os adaptadores jOOQ são beans (`@Repository`) no próprio `infrastructure/persistence`, porque o jOOQ não pode sair desse pacote (ArchUnit). Os casos de uso são montados em `infrastructure/config`:

```java
@Configuration
class ExecutionModuleConfig {
  @Bean
  FinishSession finishSession(SessionRepository sessions, CoachingLinkQuery links,
                              OutboxPublisher outbox, Clock clock) {
    return new FinishSessionUseCase(sessions, links, outbox, clock);
  }
}
```

---

## 4. Domínio rico (Rich Domain Model)

**Regras:**

1. Entidade tem **comportamento**, não só getters. A regra mora no método que muda o estado: `session.finish(...)`, `link.activate(...)`, `workout.revise(...)`.
2. **Sem setters públicos.** Estado muda só por métodos com nome de negócio.
3. **Invariantes no construtor/fábrica.** Objeto inválido não existe. Use fábricas estáticas com nome: `Workout.createFromTemplate(...)`.
4. **Value objects** como `record` imutáveis que se validam: `Kg`, `Reps`, `Rpe`, `Effort`, `BodyRegion`, `Email`, `ClientId`. Nada de `double` solto para carga ou `String` solto para e-mail.
5. **Agregados** definem consistência: altera-se o agregado inteiro numa transação, pela raiz. Referência a outro agregado é **por id**, nunca por objeto.
6. **Sem `null` no domínio.** Ausência é `Optional` (só em retorno) ou um tipo explícito.
7. Domínio **registra eventos**; a aplicação os publica no outbox.
8. IDs gerados como UUIDv7 por uma porta (`IdGenerator`) ou recebidos do app (tabelas do offline).

**Agregados principais:**

| Agregado (raiz) | Contém | Invariantes de exemplo |
|---|---|---|
| `CoachingLink` | — | só ativa se pendente; um ativo por cliente; limite do plano |
| `Workout` | `WorkoutVersion` → `Block` → `PrescribedExercise` → `PrescribedSet` | editar treino com sessões cria nova versão; `repsMin ≤ repsMax`; Tabata = HIIT 20/10 × 8 |
| `WorkoutSession` | `PerformedExercise` → `PerformedSet`, `BlockResult` | sessão concluída tem `finishedAt`; série de aquecimento não conta volume; substituição exige exercício de origem |
| `Anamnesis` | respostas | revisada é imutável; mudança = nova versão |
| `Assessment` | medidas | `%gordura` exige método |
| `ProgressPhoto` | — | só `ready` gera link de leitura |

**Exemplo:**

```java
public final class Workout {
  private final WorkoutId id;
  private WorkoutVersion current;
  private boolean hasSessions;
  private final List<DomainEvent> events = new ArrayList<>();

  public void revise(WorkoutDraft draft, UserId by) {
    var next = current.reviseWith(draft, by);          // valida blocos, séries, métodos
    if (hasSessions) {
      current = next.asNewVersion(current.number() + 1); // histórico preservado
    } else {
      current = next.inPlace(current.number());
    }
    events.add(new WorkoutRevised(id, current.number()));
  }
}

public record Reps(int min, int max) {
  public Reps {
    if (min < 0 || max < min) throw new InvalidPrescription("reps-range-invalid");
  }
}
```

**Anti-padrões proibidos:** entidade só com getters/setters e regra num `XxxService`; regra de negócio em SQL ou trigger (exceto as garantias de integridade já no schema); `if (status.equals("ACTIVE"))` espalhado fora da entidade.

---

## 5. SOLID na prática

| Princípio | Como aplicamos aqui |
|---|---|
| **S** Responsabilidade única | Um caso de uso por classe. Controller só traduz HTTP ↔ command. Repositório só persiste. |
| **O** Aberto/fechado | Métodos de execução de bloco (`sequential`, `hiit`, `emom`, `amrap`...) como `sealed interface BlockMethod` com uma implementação por método; novo método = nova classe, sem `switch` espalhado. |
| **L** Substituição de Liskov | Adaptadores de uma porta são intercambiáveis (fake em memória nos testes, jOOQ em produção) e passam no mesmo teste de contrato. |
| **I** Segregação de interfaces | Portas pequenas: `SessionReader` e `SessionWriter` em vez de um `SessionRepository` com 20 métodos quando os consumidores são diferentes. |
| **D** Inversão de dependência | Aplicação depende de portas (`PhotoStorage`), não de `S3Client`. A infraestrutura implementa. |

---

## 6. Casos de uso e transações

- Um caso de uso = uma transação. `@Transactional` **só** na implementação do caso de uso (`application/usecase`), nunca em controller ou repositório.
- **Nenhuma chamada externa dentro da transação** (S3, push, KMS remoto, gateway de pagamento). Efeito externo = evento no outbox, executado pelo worker.
- Toda transação começa com `set_config('app.user_id', ?, true)`. Isso é feito **uma vez** pela infraestrutura (`RlsTransactionManager` em `shared`, subclasse do gerenciador de transações do Spring: toda transação, inclusive do jOOQ, passa por ele); caso de uso nunca chama isso à mão.
- Autorização em duas camadas:
  1. no caso de uso: "este usuário pode fazer isto com este aluno?" (vínculo ativo, dono do recurso);
  2. no banco: RLS (defesa em profundidade). Nunca contar só com a segunda.
- Recurso de outro usuário responde **404**, não 403, para não revelar que existe.
- Leitura pesada para tela (dashboard, listas) pode usar **query service** em `application/port/in` que devolve DTO direto do jOOQ, sem montar agregado (CQRS leve). Escrita sempre pelo agregado.

---

## 7. Erros: RFC 9457 (Problem Details)

Toda resposta de erro é `application/problem+json`, montada num único `@RestControllerAdvice` global (em `shared/infrastructure/web`). Ative `spring.mvc.problemdetails.enabled=true`.

**Formato:**

```json
{
  "type": "https://api.moveup.com.br/problems/plan-limit-reached",
  "title": "Limite de alunos do plano atingido",
  "status": 409,
  "detail": "O plano atual permite até 10 alunos ativos.",
  "instance": "/v1/invites/ABC123/accept",
  "code": "plan-limit-reached",
  "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
  "errors": [ { "field": "reps.max", "code": "must-be-gte-min", "message": "..." } ]
}
```

- `code`: estável, kebab-case, é o que o frontend usa para decidir a mensagem. Nunca mudar um `code` existente.
- `type`: URI derivada do `code`. A página não precisa existir no MVP.
- `traceId`: sempre presente, para cruzar com logs.
- `errors[]`: só em validação (400/422).
- **Nunca** expor stack trace, SQL, nome de tabela, classe Java ou dado de saúde em `detail`.

**Mapeamento:**

| Origem | Status | `code` (exemplos) |
|---|---|---|
| Bean Validation no DTO (formato) | 400 | `validation-failed` |
| JSON malformado | 400 | `malformed-request` |
| `DomainException` (regra violada) | 422 | `reps-range-invalid`, `anamnesis-already-reviewed` |
| Não encontrado ou sem acesso | 404 | `resource-not-found` |
| Sem autenticação | 401 | `unauthenticated` |
| Autenticado sem papel exigido | 403 | `forbidden` |
| Concorrência (`If-Match` desatualizado) | 412 | `version-mismatch` |
| Conflito de estado | 409 | `invite-expired`, `plan-limit-reached`, `client-already-linked` |
| Rate limit | 429 | `rate-limited` |
| Inesperado | 500 | `internal-error` (detalhe só no log) |

**Erros vindos do banco** (funções como `accept_invite`) usam `SQLSTATE` próprios e são traduzidos num único lugar (`PostgresErrorTranslator`): `P0002 → invite-expired (404/409)`, `P0003 → plan-limit-reached (409)`, `P0004 → client-already-linked (409)`, `23505 unique_violation → conflito específico por constraint`.

**Esqueleto:**

```java
@RestControllerAdvice
class GlobalProblemHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler(DomainException.class)
  ProblemDetail domain(DomainException ex, HttpServletRequest req) {
    return problem(HttpStatus.UNPROCESSABLE_ENTITY, ex.code(), ex.getMessage(), req);
  }

  @ExceptionHandler(ResourceNotFound.class)
  ProblemDetail notFound(ResourceNotFound ex, HttpServletRequest req) {
    return problem(HttpStatus.NOT_FOUND, "resource-not-found", "Recurso não encontrado.", req);
  }

  @ExceptionHandler(Exception.class)
  ProblemDetail unexpected(Exception ex, HttpServletRequest req) {
    log.error("unexpected_error", ex);                  // sem payload da requisição
    return problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error", "Erro inesperado.", req);
  }

  private ProblemDetail problem(HttpStatus s, String code, String detail, HttpServletRequest req) {
    var pd = ProblemDetail.forStatusAndDetail(s, detail);
    pd.setType(URI.create("https://api.moveup.com.br/problems/" + code));
    pd.setTitle(Titles.of(code));
    pd.setInstance(URI.create(req.getRequestURI()));
    pd.setProperty("code", code);
    pd.setProperty("traceId", traceIds.current());
    return pd;
  }
}
```

Exceções de domínio carregam só `code` e mensagem segura. Título legível vem de um catálogo (`Titles`), pronto para i18n.

---

## 8. API REST

- Prefixo `/v1`. Mudança incompatível = `/v2` da rota afetada, com período de convivência.
- Recursos no plural, kebab-case: `/v1/clients/{id}/workout-sessions`. Ações que não são CRUD como sub-recurso verbal: `POST /v1/invites/{code}/accept`.
- JSON em `camelCase`. Datas ISO-8601 em UTC (`2026-10-01T12:00:00Z`); datas sem hora como `2026-10-01`.
- Unidades canônicas no contrato: kg, cm, metros, segundos. Conversão é da interface.
- IDs UUIDv7 como string. Nunca expor id sequencial.
- **Paginação por cursor** (`?cursor=...&limit=50`, máx. 100), resposta `{ items, nextCursor }`. Sem `offset`.
- **Concorrência otimista:** `ETag` no GET e `If-Match` no PUT/PATCH dos recursos editáveis pelo profissional no app (treino, programa). Divergência = 412.
- **Idempotência:** `POST` que cria efeito externo (cobrança, convite reenviado) aceita `Idempotency-Key`. O sync é idempotente por id.
- DTOs de request/response são `record` em `infrastructure/web`, **nunca** entidades de domínio. Mapper explícito (MapStruct ou método estático).
- `Cache-Control: no-store` em toda resposta com dado de saúde.

---

## 9. Documentação (OpenAPI / Swagger)

- **springdoc-openapi**. Toda rota com `@Operation(summary, description)`, todos os códigos de resposta com `@ApiResponse`, incluindo os de erro com schema `ProblemDetail`.
- Exemplos (`@ExampleObject`) nas rotas principais (sync, sessão, treino).
- Segurança declarada (`bearerAuth`) no `OpenAPIDefinition`.
- O contrato é **artefato versionado**: `backend/openapi/openapi.yaml` é gerado no build (`springdoc-openapi-maven-plugin`) e commitado. O CI falha se o arquivo gerado divergir do commitado (mudança de contrato precisa ser intencional e revisada).
- O frontend gera o cliente e os schemas Zod a partir desse arquivo (ver FRONTEND-PATTERN.md). Mudou o contrato, regenera o cliente no mesmo PR.
- Swagger UI habilitado em `local` e `staging`; **desabilitado em produção**.
- Javadoc só onde o "porquê" não é óbvio. Decisões de arquitetura vão em `docs/ARQUITETURA.md`.

---

## 10. Banco de dados

**Migrations (Flyway):**

- Arquivos em `backend/src/main/resources/db/migration`, nome em inglês e snake_case: `V<n>__short_description.sql` (ex.: `V13__client_keys_tombstones.sql`).
- **Nunca editar migration já aplicada** em qualquer ambiente compartilhado. Corrige-se com uma nova.
- Mudança que quebra compatibilidade usa **expand → migrate → contract** (adiciona coluna nova, código usa as duas, remove a antiga numa migration posterior). Deploy nunca depende de parar a aplicação.
- Toda tabela nova com `client_id` entra no RLS (V12 é o modelo) **e** ganha teste de RLS (seção 12).
- Todo FK usado em filtro tem índice. Todo `client_id` repetido é protegido por FK composta `(id, client_id)`.

**Convenções do schema:** `snake_case`; PK `uuid` (UUIDv7); datas `timestamptz` em UTC; dinheiro em centavos (`int`/`bigint`); unidades canônicas; `text + check` para enumerações (ou `domain` quando reutilizado); histórico nunca apagado com `DELETE` (status, `archived_at`, `deleted_at`).

**Acesso a dados (jOOQ):**

- Código jOOQ **gerado a partir do schema real** (Flyway aplicado num Postgres do Testcontainers durante o build). Nunca escrever nome de tabela/coluna como string.
  - Quem gera é `backend/codegen/JooqCodegen.java`, na fase `generate-sources`. Ele monta o banco como em produção: os papéis já existem e o Flyway roda como `moveup_owner`. O código sai em `br.com.moveup.shared.infrastructure.persistence.jooq` (fora do git e da cobertura).
  - Todos os módulos importam dali, mas cada um só usa as **próprias tabelas** no seu `infrastructure/persistence`. O ArchUnit não consegue checar dono de tabela, então isso fica para a revisão do PR.
- jOOQ só em `infrastructure/persistence`. Repositório mapeia registro ↔ agregado explicitamente.
- Sem N+1: carregar agregado com o mínimo de consultas (`multiset` do jOOQ para coleções).
- Paginação por chave (*keyset*), nunca `offset`.
- Upsert do sync com `onConflict(ID).doUpdate()...where(excluded.CLIENT_UPDATED_AT.gt(...))`.
- Transações curtas. Nada de chamada externa ou espera dentro delas.
- Consulta que roda por requisição em tabela grande precisa de `EXPLAIN` revisado no PR.

**Criptografia de campo:** campos sensíveis (`anamnesis.answers`, `health_restriction.description`, `pain_report.description`, `session_feedback.comment`, `client_note.body`) passam pela porta `FieldCipher` (Tink, DEK por aluno). O domínio trabalha com o valor em claro; cifra/decifra acontece no adaptador de persistência.

---

## 11. Eventos e outbox

- Evento de domínio é `record` imutável, nome no passado: `SessionFinished`, `PhotoUploaded`, `LinkEnded`. Tipo no outbox: `session.finished`.
- Payload mínimo: ids e o necessário para o handler. **Sem dado de saúde no payload.**
- Gravado no `outbox_event` **na mesma transação** da mudança de estado.
- Handlers (`infrastructure/messaging`) são **idempotentes**: processar o mesmo evento duas vezes não muda o resultado (upsert, checagem de estado).
- Falha: o poller incrementa `attempts`, agenda `next_attempt_at` com backoff exponencial e registra `last_error` (sem dado pessoal). Após N tentativas, alerta operacional.
- Jobs recorrentes (inatividade, adesão, arquivamento) com db-scheduler, também idempotentes.

---

## 12. Testes

**Pirâmide e ferramentas:**

| Nível | O que testa | Ferramentas | Regra |
|---|---|---|---|
| Domínio | Invariantes e comportamento das entidades | JUnit 5, AssertJ | Sem Spring, sem mocks. Rápido. Cobertura ≥ 90% |
| Caso de uso | Orquestração, autorização, eventos emitidos | JUnit 5 + **fakes em memória** das portas de saída | Mockito só para portas externas difíceis (S3, push) |
| Adaptador de persistência | SQL, mapeamento, constraints, upsert | **Testcontainers PostgreSQL 16** com as migrations reais | Nunca H2 |
| RLS | Isolamento entre usuários | Testcontainers, conectando como `app_api` com `set_config` | Obrigatório para toda tabela com `client_id` |
| Web | Contrato HTTP, validação, `ProblemDetail` | `@WebMvcTest` + MockMvc | Todo `code` de erro tem teste |
| Arquitetura | Camadas e fronteiras de módulo | ArchUnit | Quebra o build |
| Contrato | OpenAPI gerado = commitado | plugin + diff no CI | Quebra o build |
| Ponta a ponta (poucos) | Fluxos críticos: convite, sessão offline, upload de foto | `@SpringBootTest` + Testcontainers | Só caminhos críticos |

**Regras:**

- Nome do teste descreve comportamento: `deveCriarNovaVersaoQuandoTreinoJaTemSessoes`.
- Estrutura *given / when / then* visível (blocos ou comentários).
- **Test data builders** por agregado (`aWorkout().withSessions().build()`); nada de montar objeto gigante em cada teste.
- Fakes de porta em `src/test/.../fakes`, reaproveitados, e validados pelo mesmo teste de contrato do adaptador real.
- Relógio e ids injetados (`Clock`, `IdGenerator`), nunca `Instant.now()` direto no domínio.
- Bug corrigido = teste que reproduz o bug no mesmo PR.
- Cobertura mínima no CI (JaCoCo): 80% geral, 90% em `domain`.

**Regras ArchUnit mínimas:**

```java
@ArchTest static final ArchRule domainIsPure =
  noClasses().that().resideInAPackage("..domain..")
    .should().dependOnClassesThat().resideInAnyPackage(
      "org.springframework..", "org.jooq..", "jakarta..", "com.fasterxml..",
      "..application..", "..infrastructure..");

@ArchTest static final ArchRule applicationIgnoresInfrastructure =
  noClasses().that().resideInAPackage("..application..")
    .should().dependOnClassesThat().resideInAPackage("..infrastructure..");

@ArchTest static final ArchRule jooqOnlyInPersistence =
  noClasses().that().resideOutsideOfPackage("..infrastructure.persistence..")
    .should().dependOnClassesThat().resideInAPackage("org.jooq..");

// fronteira de módulo: <modulo>.(domain|application|infrastructure) só acessível pelo próprio módulo;
// outros módulos usam apenas <modulo>.api
```

---

## 13. Segurança no código

- Nunca logar: corpo de requisição, token, e-mail completo, nome de aluno, qualquer campo de saúde. Logue ids.
- Logs estruturados (JSON) com `traceId`, `userId` (uuid), `route`, `status`, `durationMs`.
- Segredos só por variável de ambiente/Secrets Manager. Nada em `application.yml` versionado.
- Visualização de foto e de anamnese grava `audit_log` (porta `AuditTrail`) na mesma transação.
- Texto de push e e-mail é neutro ("Carlos enviou um alerta importante").
- Dependências novas passam por revisão (licença, manutenção, CVEs); Dependabot + OWASP dependency-check no CI.

---

## 14. Estilo de código

- Java 21: `record` para DTOs, commands e value objects; `sealed` para hierarquias fechadas; *pattern matching* em `switch`.
- Injeção por construtor, campos `final`. Sem `@Autowired` em campo.
- `Optional` só como retorno. Nunca como parâmetro ou campo.
- Sem `Lombok` no `domain` (comportamento explícito). Opcional nos DTOs; preferir `record`.
- Formatação automática com **Spotless** (google-java-format); o CI falha se não estiver formatado.
- Identificadores em inglês (código e banco). Mensagens ao usuário e docs em português.
- Commits no padrão Conventional Commits: `feat(execution): ...`, `fix(coaching): ...`.

---

## 15. Checklist de PR (backend)

- [ ] Regra de negócio nova está no domínio, com teste de domínio.
- [ ] Caso de uso novo tem teste com fakes, cobrindo autorização (sem vínculo, vínculo encerrado).
- [ ] Nenhuma chamada externa dentro de transação; efeitos externos via outbox.
- [ ] Erros novos têm `code` estável, mapeamento no handler e teste de `ProblemDetail`.
- [ ] Migration nova não edita antiga; tabela com `client_id` tem RLS e teste de RLS.
- [ ] OpenAPI regenerado e commitado; cliente do frontend regenerado se o contrato mudou.
- [ ] Nada de dado de saúde em log, evento, push ou mensagem de erro.
- [ ] ArchUnit, Spotless e cobertura passando.
- [ ] `docs/` atualizado se alguma regra ou decisão mudou.
