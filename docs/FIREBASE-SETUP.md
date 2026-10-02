# Configurar o Firebase Auth

Passo a passo para criar o projeto do Firebase que o MoveUp usa para login (decisão de 01/10/2026 no [PLANO.md](PLANO.md)). Só o **login** fica no Firebase; os dados continuam no nosso Postgres.

> Nada daqui é segredo de servidor: o ID do projeto e os arquivos de configuração do app são públicos por natureza (vão dentro do app). Mesmo assim, **não cole chaves no chat**; quando um passo pedir um arquivo, salve no caminho indicado.

## 1. Criar o projeto

1. Acesse <https://console.firebase.google.com> e clique em **Criar um projeto**.
2. Nome: `moveup` (o Firebase sugere um ID como `moveup-1a2b3`; anote esse **ID do projeto**).
3. Google Analytics: **desligado** (o app não usa; menos dado pessoal circulando).

## 2. Ligar os métodos de login

Em **Authentication → Sign-in method → Adicionar novo provedor**:

1. **E-mail/senha**: ativar. Deixe "Link de e-mail (login sem senha)" desligado.
2. **Google**: ativar e escolher o e-mail de suporte do projeto.
3. **Apple**: ativar. A configuração completa (Service ID, chave) depende da conta de desenvolvedor da Apple; dá para fazer depois, antes de publicar na App Store.

Em **Authentication → Configurações**:

- **Proteção contra enumeração de e-mails**: ligada.
- **Domínios autorizados**: deixe só os padrões por enquanto.

## 3. Registrar os apps

Em **Configurações do projeto → Seus apps**:

1. **Android**: nome do pacote `br.com.moveup`. Baixe o `google-services.json`.
2. **iOS**: ID do pacote `br.com.moveup`. Baixe o `GoogleService-Info.plist`.

Salve os dois em `apps/mobile/firebase/` (a pasta será criada na Fase 1, PR F1-4).

## 4. Ligar a API ao projeto

No `.env` da raiz do repositório (nunca commitado), troque o valor de exemplo:

```
FIREBASE_PROJECT_ID=<ID do projeto do passo 1>
```

A API só aceita tokens cujo `aud` seja esse ID e cujo emissor seja `https://securetoken.google.com/<ID>` (ver `FirebaseJwt.java`). Em produção, o mesmo valor vai como variável de ambiente.

## 5. Avisar no chat

Basta mandar o **ID do projeto** (não é segredo). Com ele, o PR F1-4 liga o login do app.
