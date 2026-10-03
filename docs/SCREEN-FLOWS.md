# Fluxos de Telas

Fluxos do MVP no **app único** (`apps/mobile`), que tem dois perfis: **aluno** e **profissional**. Não existe painel web. Rotas e nomes de feature em inglês, textos de tela em português. Os diagramas usam Mermaid e aparecem como imagem no GitHub.

Referências: [FRONTEND-PATTERN.md](FRONTEND-PATTERN.md) (estrutura e regras) · [ARQUITETURA.md](ARQUITETURA.md) (sync, fotos, segurança) · [PLANO.md](PLANO.md) (em que fase cada fluxo entra).

**Legenda dos diagramas:** retângulo = tela · losango = decisão · retângulo arredondado = ação do sistema · `[[ ]]` = ação que exige internet.

---

## Parte 0 — Entrada no app e perfis

### 0.1 Mapa de rotas (Expo Router)

```
app/
├── _layout.tsx                     Decide o grupo de rotas pelo papel do usuário logado
├── (auth)/
│   ├── welcome.tsx                 "Sou personal" / "Tenho um convite"
│   ├── sign-in.tsx                 Entrar (Google, Apple, e-mail)
│   └── i/[code].tsx                Link do convite (moveup-site.pages.dev/i/CODIGO): guarda o código e segue a entrada
├── (client)/                       PERFIL ALUNO
│   ├── onboarding/{consent,accept-invite,anamnesis}.tsx
│   ├── (tabs)/{home,training,history,progress,profile}.tsx
│   ├── history/[sessionId].tsx
│   └── progress/{assessment/new,photos/new}.tsx
├── (professional)/                 PERFIL PROFISSIONAL
│   ├── onboarding/{profile,plan}.tsx
│   ├── (tabs)/{dashboard,clients,training,alerts,settings}.tsx
│   ├── clients/new.tsx
│   ├── invite-share.tsx            Código, QR Code, WhatsApp e compartilhar do convite
│   ├── clients/[id]/{overview,program,sessions,assessments,anamnesis,notes}.tsx
│   ├── training/workouts/[id]/edit.tsx
│   ├── training/templates.tsx
│   └── exercise-library.tsx
└── session/[id]/                   COMPARTILHADO: execução (aluno e presencial do profissional)
    ├── index.tsx
    ├── feedback.tsx
    └── summary.tsx
```

- Uma conta tem **um** papel no MVP. O `_layout.tsx` raiz redireciona para `(client)` ou `(professional)`; um grupo não acessa as rotas do outro.
- Telas do profissional têm layout de **tablet** (duas colunas) no editor de treino e no perfil do aluno.

### 0.2 Primeira abertura

```mermaid
flowchart TD
  open(["Abre o app"]) --> logged{"Já está logado?"}
  logged -- "sim" --> role{"Papel da conta"}
  role -- "aluno" --> chome["Home do aluno"]
  role -- "profissional" --> pdash["Dashboard do profissional"]
  logged -- "não" --> welcome["Boas-vindas<br/>'Sou personal' ou 'Tenho um convite'"]
  welcome -- "Tenho um convite" --> code["Digitar código<br/>(ou abrir pelo link)"] --> clientFlow(["Fluxo do aluno: 1.2"])
  welcome -- "Sou personal" --> psign["Criar conta<br/>Google, Apple ou e-mail"]
  psign --> pconsent["Termos e privacidade"]
  pconsent --> pprofile["Seu perfil<br/>nome, CREF (opcional), nome do negócio"]
  pprofile --> trial(["Cria organização +<br/>assinatura de teste"])
  trial --> firstInvite["'Convide seu primeiro aluno'"] --> pdash
```

---

## Parte 1 — Perfil do aluno

### 1.1 Telas do aluno

| Tela | Feature | Funciona offline? | Endpoints |
|---|---|---|---|
| sign-in, invite, accept-invite | `auth`, `invite` | Não | provedor de login, `POST /v1/invites/{code}/accept` |
| consent, anamnesis | `invite`, `anamnesis` | Não (envio) | `POST /v1/consents`, `POST /v1/anamnesis` |
| home, training | `training` | Sim (SQLite) | `GET /v1/sync` |
| session/* | `execution` | **Sim** | `POST /v1/sync` |
| history | `history` | Sim (o que já sincronizou) | `GET /v1/sync` |
| progress | `progress` | Leitura sim; fotos não | `POST /v1/assessments`, `POST /v1/photos` |
| profile | `auth`, `anamnesis` | Parcial | vários |

### 1.2 Primeiro acesso (convite → onboarding)

```mermaid
flowchart TD
  link(["Aluno toca no link ou lê o QR do convite"]) --> installed{"App instalado?"}
  installed -- "não" --> store["Loja (App Store / Play Store)"] --> open["Abre o app com o código guardado"]
  installed -- "sim" --> open
  open --> signed{"Já está logado?"}
  signed -- "não" --> signin["Entrar<br/>Google, Apple ou e-mail"]
  signin --> consent
  signed -- "sim" --> consent["Consentimentos<br/>termos, privacidade, dados de saúde<br/>fotos (opcional)"]
  consent --> accept[["Confirmar vínculo<br/>'Ana Souza quer ser seu personal'"]]
  accept --> result{"Resposta da API"}
  result -- "ok" --> anam["Anamnese<br/>objetivo, PAR-Q, lesões, dores,<br/>medicamentos, disponibilidade"]
  result -- "invite-expired" --> errExp["Convite expirado<br/>'Peça um novo link ao seu personal'"]
  result -- "plan-limit-reached" --> errLim["Personal sem vagas<br/>'Avise seu personal'"]
  result -- "client-already-linked" --> errLinked["Você já tem um personal ativo<br/>'Encerre o vínculo atual em Perfil'"]
  anam --> parq{"Algum 'sim' no PAR-Q?"}
  parq -- "sim" --> clearance["Aviso: recomendada liberação médica<br/>(personal é avisado)"] --> sync
  parq -- "não" --> sync(["Primeira sincronização<br/>baixa programa e treinos"])
  sync --> home["Home"]
```

- Sem convite, o app abre na tela "Peça o link ao seu personal", com campo para digitar o código. No MVP o aluno não usa o app sem vínculo.
- A anamnese pode ser salva como rascunho e terminada depois. Enquanto não for enviada, a Home mostra um aviso fixo.

### 1.2b Aluno menor: autorização do responsável (LGPD, art. 14)

Decisão de 03/10/2026: o menor só **indica** o responsável; quem autoriza é o próprio responsável,
pelo link que recebe no celular dele. Sem autorização, o menor não aceita convite.

```mermaid
flowchart TD
  consent["Consentimentos do menor"] --> who["'Quem autoriza?'<br/>nome e parentesco do responsável"]
  who --> share[["App cria o pedido e abre o compartilhamento<br/>(WhatsApp, SMS...) com o link"]]
  share --> wait["'Falta a Marta.'<br/>app confere a cada 10 s"]
  wait -- "Mandar o link de novo" --> share
  wait -- "Trocar responsável" --> who
  share -.-> page["Responsável abre moveup-site.pages.dev/autorizar/#segredo<br/>(sem login)"]
  page --> decide{"Autoriza?"}
  decide -- "sim" --> free(["App do menor libera sozinho<br/>e segue para o convite"])
  decide -- "não" --> declined["Menor vê 'Marta não autorizou'<br/>e pode pedir de novo"] --> who
```

- O link vale 7 dias e uma vez; reenviar troca o link (o anterior para de valer). O banco guarda
  só o hash do segredo, e a página manda o segredo no corpo da requisição (nunca na URL).
- A página mostra só o primeiro nome do menor e o que o app guarda; nenhum dado de saúde.
- Não guardamos telefone nem e-mail do responsável: o link vai pelo celular do menor.

### 1.3 Treino do dia e execução (offline-first)

```mermaid
flowchart TD
  home["Home<br/>card 'Seu treino de hoje'"] --> active{"Existe sessão em andamento?"}
  active -- "sim" --> resume["Retomar treino<br/>(app foi fechado no meio)"] --> exec
  active -- "não" --> preview["Prévia do treino<br/>blocos, exercícios, ~50 min,<br/>avisos de restrição"]
  preview --> start(["Iniciar: cria sessão local<br/>id UUIDv7, status in_progress"])
  start --> exec["Execução<br/>bloco atual, exercício, séries<br/>pré-preenchidas com o planejado"]
  exec --> action{"Ação do aluno"}
  action -- "confirmar série" --> log(["Grava série no SQLite<br/>+ fila de sync"]) --> rest["Timer de descanso<br/>(funciona com tela bloqueada)"] --> exec
  action -- "ajustar carga/reps" --> log
  action -- "pular exercício" --> skip(["status skipped"]) --> exec
  action -- "trocar exercício" --> sub["Escolher substituto<br/>(biblioteca)"] --> exec
  action -- "finalizar" --> partial{"Fez tudo?"}
  partial -- "sim" --> feedback
  partial -- "não" --> confirm["'Finalizar com exercícios pendentes?'"] --> feedback
  feedback["Como foi seu treino?<br/>esforço 0–10, comentário,<br/>sentiu dor? onde? em qual exercício?"]
  feedback --> close(["Fecha a sessão local<br/>completed ou partial"])
  close --> summary["Resumo<br/>duração, volume, grupos musculares,<br/>comparação com a última vez, recordes"]
  summary --> home
  close -.-> bg(["Sync em segundo plano<br/>quando houver internet"])
```

**Telas de execução por método de bloco:**

| Método | O que a tela mostra | Timer |
|---|---|---|
| Sequencial | Um exercício por vez, lista de séries | Descanso entre séries |
| Biset / Triset | Os 2–3 exercícios do round lado a lado | Descanso ao fim do round |
| Circuito | Lista do circuito com o atual destacado, contador de rounds | Transição e descanso entre rounds |
| HIIT / Tabata | Exercício atual em tela cheia | Trabalho/descanso automático com bipes |
| EMOM | Tarefa do minuto | Relógio de minuto em minuto |
| AMRAP | Lista de tarefas + contador de rounds e reps extras | Regressivo do tempo total |
| Intervalado | Estímulo atual (forte/leve), distância ou tempo | Por intervalo |

**Regras da tela de execução:** botões grandes, uma ação principal por vez, sem depender de internet, aviso discreto "sem conexão — tudo será enviado depois" quando offline. Se houver restrição ativa (ex.: joelho), o exercício afetado mostra o aviso do personal.

### 1.4 Estados da sessão e do sync

```mermaid
stateDiagram-v2
  [*] --> in_progress: Iniciar
  in_progress --> completed: Finalizar (tudo feito)
  in_progress --> partial: Finalizar com pendências
  in_progress --> abandoned: Descartar
  completed --> completed: Editar depois (marca edited_after_finish_at)
  partial --> partial: Editar depois
```

```mermaid
stateDiagram-v2
  [*] --> pending: Gravou no SQLite
  pending --> syncing: Internet disponível
  syncing --> synced: API confirmou
  syncing --> pending: Falhou (tenta de novo com espera crescente)
  synced --> pending: Editou de novo
```

Na tela de Histórico, sessões ainda não enviadas mostram um ícone de nuvem com relógio. Nada é bloqueado por isso.

### 1.5 Avaliação e fotos

```mermaid
flowchart TD
  prog["Evolução<br/>gráficos de peso, medidas, carga"] --> choose{"O que registrar?"}
  choose -- "medidas" --> assess["Nova avaliação<br/>peso, % gordura + método, medidas"] --> saveA[["Salvar"]] --> prog
  choose -- "fotos" --> hasConsent{"Consentimento de fotos ativo?"}
  hasConsent -- "não" --> askConsent["Explica uso e privacidade<br/>'Só você e seu personal veem'"] --> grant{"Aceita?"}
  grant -- "não" --> prog
  grant -- "sim" --> camera
  hasConsent -- "sim" --> camera["Câmera guiada<br/>frente, lado, costas<br/>(silhueta de referência)"]
  camera --> upload[["Envio: pede link, envia direto,<br/>confirma"]]
  upload --> processing["Foto em processamento<br/>(status pending)"]
  processing --> ready{"Worker terminou?"}
  ready -- "ready" --> compare["Comparação<br/>inicial × atual, lado a lado"]
  ready -- "rejected" --> rejected["'Não conseguimos usar esta foto'<br/>tentar de novo"]
```

- Envio de fotos exige internet. Sem internet, o botão fica desabilitado com explicação; as fotos **não** ficam guardadas no rolo da câmera do aparelho.
- Telas de fotos bloqueiam captura de tela (se adotado) e não aparecem no session replay.

### 1.6 Perfil, privacidade e saída

```mermaid
flowchart TD
  profile["Perfil"] --> data["Meus dados"]
  profile --> anam["Minha anamnese<br/>ver versões, atualizar"]
  profile --> privacy["Privacidade<br/>consentimentos, exportar meus dados"]
  profile --> link["Meu personal<br/>encerrar vínculo"]
  profile --> logout(["Sair: apaga SQLite e tokens<br/>(avisa se houver sessão não enviada)"])
  profile --> delete["Excluir conta"] --> confirmDel["Confirmação em 2 passos<br/>explica o que é apagado"] --> doDel[["Pedido de exclusão"]] --> bye(["Logout e tela de despedida"])
```

---

## Parte 2 — Perfil do profissional

### 2.1 Telas do profissional

| Aba / tela | Feature | Funciona offline? | Observação |
|---|---|---|---|
| Dashboard | `alerts`, `clients` | Leitura do cache | Indicadores + "N alunos precisam de atenção" |
| Alunos (lista, novo, perfil com abas) | `clients` | Leitura do cache | Abas: resumo, programa, sessões, avaliações, anamnese, notas |
| Treinos (templates, editor) | `training` | Edição sim, **salvar exige internet** | Rascunho local se a conexão cair |
| Biblioteca de exercícios | `exercise-library` | Leitura do cache | Busca sem acento |
| Atenção | `alerts` | Leitura do cache | Resolver/adiar exige internet |
| Treino presencial | `execution` | **Sim** | Usa `session/[id]`, com `performedBy = professional` |
| Ajustes | `billing`, `alerts` | Não | Plano e uso (alunos ativos / limite), limites de alerta, push |

**Navegação:** 5 abas embaixo (Dashboard, Alunos, Treinos, Atenção, Ajustes). No tablet, a lista de alunos e o perfil ficam lado a lado.

### 2.2 Convidar aluno

```mermaid
flowchart TD
  list["Alunos"] --> add["Convidar aluno"] --> form["Pré-cadastro<br/>nome, e-mail/WhatsApp, objetivo"]
  form --> create[["Cria aluno + vínculo pendente + convite"]]
  create --> limit{"Plano tem vaga?"}
  limit -- "não" --> upgrade["'Você atingiu o limite de alunos'<br/>ver planos / inativar alguém"]
  limit -- "sim" --> share["Compartilhar convite<br/>link, QR Code, botão WhatsApp"]
  share --> pending["Aluno aparece como 'Pendente'<br/>(reenviar, cancelar convite)"]
  pending -. "aluno aceita no app" .-> active["Aluno 'Ativo'<br/>anamnese chega para revisão"]
  active --> review["Revisar anamnese<br/>complementar, marcar liberação médica"]
```

- A checagem de vaga acontece de novo no aceite (no banco, com trava), porque o limite pode ter sido atingido entre o convite e o aceite.

### 2.3 Montar treino e agenda

```mermaid
flowchart TD
  client["Perfil do aluno<br/>aba Programa"] --> hasProg{"Tem programa ativo?"}
  hasProg -- "não" --> newProg["Novo programa<br/>nome, objetivo, início/fim"]
  newProg --> mode{"Agenda"}
  mode -- "dias fixos" --> days["Treino A: seg e qui<br/>Treino B: ter e sex"]
  mode -- "sequência" --> seq["A → B → C<br/>meta: 4x por semana"]
  days --> workouts
  seq --> workouts
  hasProg -- "sim" --> workouts["Treinos do programa"]
  workouts --> source{"Criar treino"}
  source -- "do template" --> copy(["Copia o template para o aluno"]) --> editor
  source -- "do zero" --> editor["Editor de treino"]
  editor --> block["Adicionar bloco<br/>método: sequencial, biset, circuito,<br/>HIIT/Tabata, EMOM, AMRAP, intervalado"]
  block --> ex["Adicionar exercícios<br/>(busca na biblioteca)"]
  ex --> warn{"Exercício envolve região<br/>com restrição ativa?"}
  warn -- "sim" --> showWarn["Aviso: 'Lesão no joelho direito'<br/>(não bloqueia)"] --> sets
  warn -- "não" --> sets["Séries: tipo, reps (faixa), carga,<br/>descanso, RPE/RIR alvo"]
  sets --> save[["Salvar (If-Match)"]]
  save --> saved{"Resposta"}
  saved -- "ok" --> version(["Se o treino já tinha sessões,<br/>vira nova versão"]) --> done["Treino publicado<br/>aluno recebe no próximo sync"]
  saved -- "version-mismatch" --> conflict["'Este treino foi alterado em outro aparelho'<br/>recarregar e comparar"]
  saved -- "sem internet" --> draft(["Rascunho fica salvo no aparelho<br/>'Salvaremos quando a conexão voltar'"])
```

- "Salvar como template" fica disponível no editor a qualquer momento.
- **Editor no celular:** um bloco por cartão; adicionar exercício e editar séries abrem em folha inferior (*bottom sheet*); reordenar com toque longo e arrastar. A gravação é única, no Salvar.
- **No tablet:** lista de blocos à esquerda e edição do bloco à direita.

### 2.4 Central de atenção

```mermaid
flowchart TD
  dash["Dashboard<br/>'3 alunos precisam de atenção'"] --> alerts["Central de atenção<br/>urgente primeiro"]
  alerts --> detail["Alerta<br/>ex.: 'Carlos relatou dor no ombro no Treino B'<br/>fatos: data, exercício, intensidade"]
  detail --> act{"Ação"}
  act -- "ver sessão" --> session["Sessão (planejado × realizado)"]
  act -- "falar com o aluno" --> wa["Abre WhatsApp com texto pronto"]
  act -- "ajustar treino" --> editor["Editor de treino"]
  act -- "resolver" --> resolved(["status resolved"])
  act -- "adiar" --> snooze["Adiar até<br/>amanhã, 3 dias, 1 semana"] --> snoozed(["status snoozed"])
```

| Alerta | Severidade | Aparece em |
|---|---|---|
| Dor relatada | Urgente (push opcional) | Topo da central, perfil do aluno |
| Liberação médica pendente | Atenção | Central, aba Anamnese |
| Novo feedback | Info | Central, aba Sessões |
| Sem treinar há N dias | Atenção | Central, lista de alunos |
| Adesão baixa | Atenção | Central, overview do aluno |
| Esforço alto repetido | Atenção | Central |
| Avaliação atrasada | Info | Central, aba Avaliações |
| Sem alteração de indicador | Info | Central |
| Sessão editada após finalizar | Info | Central, aba Sessões |

### 2.5 Treino presencial

```mermaid
flowchart LR
  client["Perfil do aluno"] --> pick["Iniciar treino presencial<br/>escolhe o treino"] --> exec["session/[id]<br/>mesma tela de execução do aluno"] --> finish["Finalizar<br/>feedback opcional"] --> summary["Resumo<br/>performed_by = professional"]
```

Funciona offline (academia sem sinal), igual ao aluno: grava no SQLite do aparelho do profissional e sincroniza depois. O aluno vê a sessão no histórico dele após o sync.

### 2.6 Plano e assinatura

```mermaid
flowchart TD
  settings["Ajustes > Plano"] --> usage["Uso atual<br/>8 de 10 alunos ativos, dias de teste restantes"]
  usage --> change["Mudar de plano"] --> pay[["Pagamento<br/>meio definido na Fase 8 do PLANO"]]
  pay --> ok{"Confirmado pela loja ou gateway?"}
  ok -- "sim" --> active(["Assinatura ativa<br/>(webhook atualiza o plano)"])
  ok -- "não" --> retry["Pagamento não concluído<br/>tentar de novo"]
  usage --> down["Plano menor com mais alunos que o limite"] --> pickInactive["Escolher quais alunos inativar"]
```

Sem painel web, a assinatura é contratada dentro do app. O meio de pagamento (compra da loja ou pagamento alternativo/link externo) é uma decisão em aberto no `PLANO.md`.

---

## Parte 3 — Estados de tela e erros

Toda tela de dados implementa os quatro estados. O texto de erro vem do catálogo por `code` (FRONTEND-PATTERN.md, seção 9).

| Tela | Vazio | Carregando | Sem internet | Erro |
|---|---|---|---|---|
| Home (aluno) | "Seu personal ainda não montou seu treino" | Skeleton do card | Mostra dados locais + aviso discreto | Dados locais + "tentar sincronizar" |
| Execução | — | — | Funciona normal | Nunca bloqueia; falha de gravação local = erro grave (Sentry) |
| Histórico | "Seus treinos aparecem aqui" | Skeleton da lista | Mostra o que já sincronizou | Idem |
| Evolução | "Registre sua primeira avaliação" | Skeleton dos gráficos | Leitura local; fotos desabilitadas | Por bloco |
| Alunos | "Convide seu primeiro aluno" + botão | Skeleton da tabela | Banner "sem conexão" | Error boundary da rota |
| Editor de treino | "Adicione o primeiro bloco" | — | Rascunho local, salva quando voltar a conexão | `version-mismatch` → tela de conflito |
| Central de atenção | "Tudo em dia por aqui" | Skeleton | Banner | Error boundary |

| `code` da API | Onde aparece | Mensagem |
|---|---|---|
| `invite-expired` | Aceite de convite | Convite expirado. Peça um novo link ao seu personal. |
| `plan-limit-reached` | Aceite (aluno) / convidar (personal) | Aluno: seu personal está sem vagas no momento. Personal: você atingiu o limite de alunos do seu plano. |
| `client-already-linked` | Aceite de convite | Você já tem um personal ativo. Encerre o vínculo atual em Perfil. |
| `version-mismatch` | Editor de treino | Este treino foi alterado em outro aparelho. |
| `resource-not-found` | Qualquer detalhe | Não encontramos o que você procurava. |
| `rate-limited` | Qualquer | Muitas tentativas. Aguarde um instante. |
| `internal-error` | Qualquer | Algo deu errado. Tente de novo. |

---

## Fora do MVP (não desenhar ainda)

Chat, smartwatch, periodização por semanas, Studio com vários profissionais, marketplace de programas, cobrança dos alunos pelo personal.
