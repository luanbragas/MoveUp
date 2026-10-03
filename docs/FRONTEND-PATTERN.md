# Padrões do Frontend

Regras obrigatórias para `apps/mobile`: **um único app** (Expo, iOS e Android, celular e tablet) com dois perfis, **aluno** e **profissional**. Não existe painel web. Quando uma regra precisar mudar, muda **este arquivo primeiro**, no mesmo PR. Rotas e fluxos de cada tela estão em [SCREEN-FLOWS.md](SCREEN-FLOWS.md).

**Stack:** TypeScript (strict) · React Native + Expo + Expo Router · TanStack Query · Zod · expo-sqlite + Drizzle · React Hook Form · Zustand (pouco) · ESLint + Prettier · Jest/Vitest · Testing Library · MSW.

---

## 1. Princípios

1. **Feature primeiro.** O código é organizado pelo que o usuário faz (treino, execução, evolução), não por tipo de arquivo.
2. **UI não conhece de onde vem o dado.** Tela chama hook; hook chama porta; adaptador decide se é SQLite ou API.
3. **Nada entra sem validação.** Todo dado que cruza uma borda (API, SQLite, formulário, rota, ambiente) passa por Zod.
4. **Offline é o caso normal** no app do aluno, não exceção.
5. **Dado de saúde não sai do aparelho** a não ser pela API: nunca em log, Sentry, PostHog ou notificação.

---

## 2. Monorepo

```
apps/
  mobile/            ← o app (Expo): perfil aluno + perfil profissional
site/                ← página estática: link do convite, termos, privacidade (sem chamar a API)
packages/
  api-client/        ← GERADO a partir de backend/openapi/openapi.yaml (tipos + schemas Zod + cliente fetch)
  domain/            ← regras puras compartilhadas (volume, pace, unidades, adesão); sem React
  config/            ← tsconfig, eslint e prettier base
```

- Gerenciador: **pnpm workspaces**.
- `packages/api-client` é gerado com **Orval** (modo `zod` + cliente `fetch`), sem hooks: os hooks ficam nas features. **Nunca editar à mão**; regenerar com `pnpm api:generate` sempre que o OpenAPI mudar.
- `packages/domain` não importa React, React Native, fetch nem SQLite.

---

## 3. TypeScript strict

`packages/config/tsconfig.base.json` (todos os projetos estendem):

```jsonc
{
  "compilerOptions": {
    "strict": true,
    "noUncheckedIndexedAccess": true,
    "exactOptionalPropertyTypes": true,
    "noImplicitOverride": true,
    "noImplicitReturns": true,
    "noFallthroughCasesInSwitch": true,
    "noPropertyAccessFromIndexSignature": true,
    "useUnknownInCatchVariables": true,
    "verbatimModuleSyntax": true,
    "forceConsistentCasingInFileNames": true,
    "skipLibCheck": true
  }
}
```

- **Proibido:** `any` (use `unknown` + Zod), `@ts-ignore` (use `@ts-expect-error` com justificativa, raro), *non-null assertion* (`!`), `as` para "forçar" tipo de dado externo.
- Uniões discriminadas para estados (`{ status: 'idle' } | { status: 'running', startedAt: Date } | ...`) com `switch` exaustivo.
- Tipos de domínio são derivados dos schemas Zod (`z.infer`) ou declarados em `domain/`; nunca usar o DTO da API direto na UI.
- IDs com *branded types*: `type ClientId = string & { readonly __brand: 'ClientId' }`.

---

## 4. ESLint + Prettier

- ESLint *flat config* em `packages/config/eslint`, com:
  - `typescript-eslint` `strict-type-checked` + `stylistic-type-checked`;
  - `react-hooks` (regras de hooks e dependências);
  - `eslint-plugin-boundaries` para as regras de camada e de feature (seção 5);
  - `@typescript-eslint/switch-exhaustiveness-check`, `no-floating-promises`, `consistent-type-imports`, `no-misused-promises`;
  - `no-console` (use o logger), `import/no-cycle`.
- **Prettier** para formatação (sem regra de estilo no ESLint). Config única em `packages/config/prettier`.
- **Husky + lint-staged**: lint e prettier nos arquivos alterados antes do commit.
- CI roda `pnpm typecheck && pnpm lint && pnpm test`. Warning conta como erro (`--max-warnings 0`).

---

## 5. Estrutura por feature

```
apps/mobile/src/
├── app/                         ← rotas do Expo Router (arquivos finos: só montam a tela da feature)
├── features/
│   └── training/
│       ├── ui/                  ← telas e componentes da feature
│       ├── hooks/               ← useTodayWorkout, useCurrentProgram (TanStack Query)
│       ├── domain/              ← tipos, regras puras, PORTAS (interfaces)
│       ├── data/
│       │   ├── api/             ← adapter HTTP: api-client + Zod + mapper DTO → domínio
│       │   ├── local/           ← adapter SQLite: tabelas Drizzle, queries, mapper linha → domínio
│       │   └── training-repository.ts  ← implementa a porta combinando local e api
│       └── index.ts             ← API pública da feature (o que outras podem importar)
├── shared/                      ← ui base (Button, Text), lib (http, logger, date), hooks genéricos
└── providers/                   ← composition root: QueryClient, repositórios, auth, tema
```

**Nomes:** **toda pasta e todo arquivo em inglês**, em kebab-case (`training`, `execution`, `progress`, `exercise-library`). Componentes React em PascalCase (`WorkoutCard.tsx`). Só o texto exibido ao usuário é em português.

| Feature | Perfil | Módulo do backend |
|---|---|---|
| `auth` | ambos | `accounts` |
| `invite` | ambos (profissional envia, aluno aceita) | `coaching` |
| `clients` | profissional | `coaching` |
| `training` | ambos (aluno vê; profissional monta) | `training` |
| `exercise-library` | profissional | `training` |
| `execution`, `sync` | ambos (aluno executa; profissional no presencial) | `execution` |
| `history` | ambos | `execution` |
| `progress` (avaliações e fotos) | ambos | `assessment` |
| `anamnesis` | ambos | `anamnesis` |
| `alerts` | profissional | `alerts` |
| `billing` | profissional | `billing` |

**Perfis no mesmo app:**

- Rotas separadas por grupo no Expo Router: `app/(client)/...` e `app/(professional)/...`. O layout raiz decide o grupo pelo papel do usuário logado.
- No MVP, uma conta tem **um** papel. Feature compartilhada (ex.: `execution`) não sabe qual perfil está usando; recebe o que precisa por parâmetro (`performedBy`).
- Telas do profissional precisam funcionar em **tablet** (layout de duas colunas no editor de treino e no perfil do aluno).

**Regras de dependência (verificadas pelo `eslint-plugin-boundaries`):**

| Camada | Pode importar | Não pode importar |
|---|---|---|
| `ui` | `hooks`, `domain` (tipos), `shared/ui` | `data`, api-client, SQLite |
| `hooks` | `domain` (portas e tipos), TanStack Query, `providers` (para obter a porta) | `data` direto |
| `domain` | `packages/domain`, Zod | React, React Native, fetch, SQLite, api-client |
| `data` | `domain`, api-client, Drizzle/SQLite, `shared/lib` | `ui`, `hooks` |
| outra feature | só o `index.ts` dela | `ui/`, `data/`, `domain/` internos dela |

---

## 6. Camada `data`: portas e adaptadores

A mesma ideia do backend: `domain/` declara a porta, `data/` implementa.

```ts
// features/execution/domain/ports.ts
export interface SessionRepository {
  start(input: StartSession): Promise<Session>;
  logSet(sessionId: SessionId, set: PerformedSetInput): Promise<void>;
  finish(sessionId: SessionId, feedback: Feedback): Promise<void>;
  getActive(): Promise<Session | null>;
}
```

```ts
// features/execution/data/session-repository.ts  (offline-first)
export function createSessionRepository(db: LocalDb, outbox: LocalOutbox): SessionRepository {
  return {
    async logSet(sessionId, set) {
      const row = { ...toSetRow(sessionId, set), id: uuidv7(), clientUpdatedAt: now() };
      await db.transaction(async (tx) => {
        await tx.insert(performedSets).values(row).onConflictDoUpdate(/* ... */);
        await outbox.enqueue(tx, { entity: 'performed_set', id: row.id });
      });
    },
    // ...
  };
}
```

- **Composition root** (`providers/repositories.tsx`) cria os adaptadores reais e os entrega via contexto. Hooks pegam com `useRepositories()`. Nos testes, o provider recebe **fakes**.
- **Mobile, dados de execução:** SQLite é a fonte da verdade local. Toda escrita vai ao SQLite + fila local (`outbox`); a feature `sync` envia ao servidor quando houver rede (`POST /v1/sync`) e baixa mudanças (`GET /v1/sync?since=`), aplicando por upsert e removendo os *tombstones*.
- IDs dos registros offline são **UUIDv7 gerados no aparelho**; `clientUpdatedAt` é gravado em toda edição (o servidor resolve conflito por ele).
- **Perfil profissional:** dados de gestão (alunos, alertas, treinos) vêm da API com cache do TanStack Query persistido; só o treino presencial grava no SQLite como o aluno. Edição de treino exige internet para salvar, mas o rascunho fica guardado localmente se a conexão cair.
- Mapper explícito DTO → domínio e linha SQLite → domínio. Nunca vazar DTO ou linha do banco para `hooks`/`ui`.

---

## 7. Validação nas bordas com Zod

Toda entrada externa é validada **uma vez, na borda**, e daí para dentro o tipo é confiável.

| Borda | Onde | Como |
|---|---|---|
| Resposta da API | `data/api` | `schema.parse(json)` com os schemas gerados pelo Orval (refinados quando preciso) |
| Erro da API | `shared/lib/http` | `ProblemDetailSchema.safeParse` → `AppError` |
| Linha do SQLite | `data/local` | parse no mapper (dados antigos de versões anteriores do app podem estar diferentes) |
| Formulário | `ui` | React Hook Form + `zodResolver(schema)` |
| Parâmetros de rota e deep link | `app/` / rotas | parse antes de usar |
| Variáveis de ambiente | `shared/lib/env.ts` | parse na inicialização; app não sobe com env inválido |
| Push recebido / storage | onde é lido | parse |

- Falha de parse na API é **bug de contrato**: registra no Sentry (sem payload) e mostra erro genérico.
- Schemas de formulário reaproveitam as regras do domínio (`repsMin <= repsMax`) via `packages/domain`.

---

## 8. Estado

| Tipo de estado | Ferramenta |
|---|---|
| Dado do servidor (lista de alunos, treinos, alertas) | **TanStack Query** |
| Dado local persistente (sessão, séries, fila de sync) | SQLite, lido por hooks com TanStack Query (`queryFn` lê o SQLite) e invalidado após escrita/sync |
| Estado de UI de uma tela | `useState` / `useReducer` |
| Estado global de cliente pequeno (timer da sessão em andamento, preferências) | **Zustand**, um store por necessidade |
| Formulário | React Hook Form |

**TanStack Query:**

- `queryKey` só via **fábrica por feature** (`trainingKeys.today(clientId)`); nunca array escrito à mão no componente.
- Mobile: `networkMode: 'offlineFirst'`, `onlineManager` ligado ao NetInfo e `focusManager` ao `AppState`.
- `staleTime` definido por recurso (não deixar o padrão 0 em tudo).
- Mutação do perfil profissional com atualização otimista só quando o rollback é simples; senão, invalidar.
- Erros chegam como `AppError` (seção 9); `retry` desligado para 4xx.

Proibido: Redux, Context para estado que muda com frequência, copiar dado do servidor para Zustand.

---

## 9. Erros

- `shared/lib/http` converte toda resposta de erro em `AppError` a partir do `ProblemDetail` (RFC 9457) do backend: `{ kind: 'problem', code, status, traceId } | { kind: 'network' } | { kind: 'unexpected' }`.
- A mensagem ao usuário vem de um **catálogo por `code`** (`shared/ui/error-messages.ts`), em português. Não exibir `detail` cru.
- `network` no mobile não é erro para dados de execução (fica na fila); é erro só para ações que exigem servidor (aceitar convite, pagamento).
- Error boundary por rota com botão de tentar de novo.

---

## 10. UI

- Componentes de tela não fazem fetch nem acessam SQLite: recebem dados do hook.
- Componentes base em `shared/ui` com *design tokens* (`theme.ts`: cores, fontes, espaçamento, raios, tipografia). Tela nova usa os tokens e os componentes base; nada de cor ou fonte solta no código da feature.
- **Sistema visual "Impacto"**, só tema escuro (fundo `#0A0A0B`). O lima (`#C6FF3D`) é ação, seleção e sucesso; o vermelho (`#FF3B3B`) só para dor, perigo e erro. Títulos em caixa alta na Archivo Expanded (uma ou duas palavras por linha); números na Archivo Black, sempre com unidade ("55 kg · 10 reps"); texto corrido na Manrope. As fontes ficam em `assets/fonts` (licença OFL) e carregam com a abertura do app.
- **Tela de execução do treino:** botões grandes (mínimo 44 pt no iOS e 48 dp no Android, preferir maior), contraste alto, poucas ações por tela, uso com uma mão.
- Acessibilidade: `accessibilityLabel`/`aria-label` em ícones, ordem de foco correta, fonte escalável.
- Listas longas com **FlashList** (mobile); imagens com `expo-image` (cache).
- Textos em português num único lugar por feature (`ui/strings.ts`), prontos para i18n.

---

## 11. Segurança no app

- Tokens só no **expo-secure-store**. Nunca AsyncStorage para token.
- O SQLite do aparelho guarda dado de saúde: avaliar criptografia do banco local (SQLCipher no `expo-sqlite`) e **apagar tudo no logout**.
- Nada de dado de saúde em `console`, Sentry (`sendDefaultPii: false`, `beforeSend` limpando), PostHog (session replay mascarado nas telas de anamnese, avaliação, fotos e feedback).
- Fotos de evolução: exibidas só por URL pré-assinada temporária, sem cache em disco público; considerar bloquear captura de tela nessas telas (`expo-screen-capture`).
- Deep links validados com Zod e com checagem de sessão.

---

## 12. Testes

| Nível | O que testa | Ferramentas |
|---|---|---|
| Domínio | Regras puras (`packages/domain`, `features/*/domain`) | Vitest (packages) / Jest (app) |
| Repositórios | Adaptadores `data/` com SQLite em memória e API com **MSW** | Jest + MSW |
| Sync | Offline → online, reenvio, conflito por `clientUpdatedAt`, tombstones | Jest com relógio e rede falsos |
| Hooks | Comportamento com repositórios **fake** | Testing Library (`renderHook`) |
| UI | Interação do usuário, acessibilidade | React Native Testing Library / Testing Library |
| E2E | Fluxos críticos: convite, executar treino offline, enviar foto, montar treino | **Maestro**, nos dois perfis, em celular e tablet |

- Teste de UI busca por papel e texto visível (`getByRole`, `getByText`), não por `testID`, salvo exceção.
- Fakes de repositório ficam em `features/*/data/fakes` e passam pelos mesmos testes de contrato do adaptador real.
- Cobertura mínima: 90% em `domain` e `sync`; 70% geral.

---

## 13. Checklist de PR (frontend)

- [ ] `pnpm typecheck`, `pnpm lint` (0 warnings) e `pnpm test` passando.
- [ ] Nenhuma tela importa `data/`, api-client ou SQLite.
- [ ] Todo dado externo novo passa por Zod na borda.
- [ ] Query keys pela fábrica da feature; `staleTime` definido.
- [ ] Erro novo do backend (`code`) tem mensagem no catálogo.
- [ ] Mudança de contrato: `pnpm api:generate` rodado e commitado.
- [ ] Fluxo offline testado (mobile) quando a feature grava dado de execução.
- [ ] Nada de dado de saúde em log, analytics ou notificação.
- [ ] Acessibilidade conferida na tela nova.
