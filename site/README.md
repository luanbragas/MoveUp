# site

Página estática mínima do MoveUp (sem chamar a API), publicada no Cloudflare Pages em
`moveup-site.pages.dev`.

```
static/                   o que vai para o ar como está
├── index.html            página inicial
├── i/index.html          link do convite: /i/<código> (o código é lido no navegador)
├── assets/               scripts da página do convite (fora de /i/ por causa do rewrite)
├── _redirects            /i/:code → /i/ (rewrite 200 do Cloudflare Pages)
└── _headers              CSP, Referrer-Policy e tipo JSON do arquivo da Apple
app-links.mjs             gera apple-app-site-association e assetlinks.json
build.mjs                 monta dist/ = static/ + .well-known/
```

## Comandos

```bash
pnpm --filter @moveup/site build:preview   # dist/ sem os arquivos de app links (para olhar a página)
pnpm --filter @moveup/site build           # produção: exige MOVEUP_ANDROID_SHA256
pnpm --filter @moveup/site test
```

## Variáveis do build (produção)

| Variável | O que é | Onde pegar |
|---|---|---|
| `MOVEUP_APPLE_TEAM_ID` | Opcional. Team ID da conta Apple Developer (10 caracteres); sem ela, no iPhone o convite entra pelo código | developer.apple.com → Membership |
| `MOVEUP_ANDROID_SHA256` | SHA-256 do certificado que assina o app; vários separados por vírgula | `eas credentials` (Android) e Play Console → Integridade do app → Assinatura |

Não são segredos (ficam públicos em `/.well-known/`), mas são da conta: configure no painel do
Cloudflare Pages, não no repositório.

## Publicar (Cloudflare Pages)

Projeto `moveup-site` (endereço grátis `moveup-site.pages.dev`, sem domínio próprio por enquanto).

- Build command: `pnpm --filter @moveup/site build:preview` até existir o primeiro build Android
  (EAS); depois `pnpm --filter @moveup/site build` com `MOVEUP_ANDROID_SHA256` no painel.
- Output directory: `site/dist`

## Trocar o domínio (ex.: ao comprar um)

O domínio do convite aparece em quatro lugares, que mudam juntos:

1. `backend/src/main/resources/application.yml` → `moveup.invite.link-base-url`
2. `apps/mobile/src/features/clients/domain/client.ts` → `INVITE_BASE_URL`
3. `apps/mobile/app.json` → `ios.associatedDomains` e `android.intentFilters[].data[].host`
4. Domínio personalizado no projeto do Cloudflare Pages

Universal links e app links só funcionam em build de desenvolvimento ou de loja (não no Expo Go):
no Expo Go, o convite entra pelo código digitado.

## Por fase

- **Fase 1:** link do convite e arquivos de universal links / app links (feito; no ar quando o
  Android tiver o certificado).
- **Fase 9:** termos de uso e política de privacidade revisados pelo jurídico; links das lojas na
  página do convite quando o app for publicado.
