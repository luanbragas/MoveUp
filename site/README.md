# site

Página estática mínima do MoveUp (sem chamar a API), publicada no Cloudflare Pages em
`moveup.com.br`.

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
pnpm --filter @moveup/site build           # produção: exige as duas variáveis abaixo
pnpm --filter @moveup/site test
```

## Variáveis do build (produção)

| Variável | O que é | Onde pegar |
|---|---|---|
| `MOVEUP_APPLE_TEAM_ID` | Team ID da conta Apple Developer (10 caracteres) | developer.apple.com → Membership |
| `MOVEUP_ANDROID_SHA256` | SHA-256 do certificado que assina o app; vários separados por vírgula | `eas credentials` (Android) e Play Console → Integridade do app → Assinatura |

Não são segredos (ficam públicos em `/.well-known/`), mas são da conta: configure no painel do
Cloudflare Pages, não no repositório.

## Publicar (Cloudflare Pages)

- Build command: `pnpm --filter @moveup/site build`
- Output directory: `site/dist`
- Domínio personalizado: `moveup.com.br` (o mesmo de `moveup.invite.link-base-url` no backend e
  de `associatedDomains` / `intentFilters` no `apps/mobile/app.json`).

Universal links e app links só funcionam em build de desenvolvimento ou de loja (não no Expo Go):
no Expo Go, o convite entra pelo código digitado.

## Por fase

- **Fase 1:** link do convite e arquivos de universal links / app links (feito).
- **Fase 9:** termos de uso e política de privacidade revisados pelo jurídico; links das lojas na
  página do convite quando o app for publicado.
