import type { ConsentKind } from "../domain/me";
import type { GuardianRelationship } from "../domain/ports";
import type { AuthErrorCode } from "../domain/session";

// Textos da feature auth (pt-BR), num lugar só e prontos para i18n.
export const strings = {
  entry: {
    error: "Não conseguimos carregar sua conta.",
    retry: "Tentar de novo",
    signOut: "Sair",
  },
  welcome: {
    title: "MoveUp",
    description: "Treinos do seu personal, no seu bolso.",
    professional: "Sou personal",
    client: "Tenho um convite",
  },
  signIn: {
    titleSignIn: "Entrar",
    titleSignUp: "Criar conta",
    subtitle: "Por enquanto com e-mail e senha. Google e Apple chegam em breve.",
    email: "E-mail",
    password: "Senha",
    submitSignIn: "Entrar",
    submitSignUp: "Criar conta",
    toggleToSignUp: "Não tenho conta",
    toggleToSignIn: "Já tenho conta",
    forgot: "Esqueci a senha",
    resetSent: "Se o e-mail estiver cadastrado, você vai receber o link para criar uma senha nova.",
    emailInvalid: "Confira o e-mail.",
    passwordShort: "A senha precisa de pelo menos 8 caracteres.",
  },
  authErrors: {
    "invalid-credentials": "E-mail ou senha incorretos.",
    "email-in-use": "Este e-mail já tem conta. Entre com ele.",
    "weak-password": "Senha fraca. Use pelo menos 8 caracteres, com letras e números.",
    "invalid-email": "Confira o e-mail.",
    "too-many-requests": "Muitas tentativas. Aguarde um pouco.",
    network: "Sem conexão com a internet.",
    unknown: "Não foi possível entrar. Tente de novo.",
  } satisfies Record<AuthErrorCode, string>,
  register: {
    title: "Seu cadastro",
    subtitle: "Conte quem você é para prepararmos o app.",
    roleLabel: "Você é",
    roleRequired: "Escolha se você é personal ou aluno.",
    professional: "Personal",
    client: "Aluno",
    name: "Nome completo",
    birthDate: "Data de nascimento (DD/MM/AAAA)",
    birthDateOptional: "Data de nascimento (opcional, DD/MM/AAAA)",
    businessName: "Nome do seu negócio (opcional)",
    registryNumber: "CREF (opcional)",
    submit: "Continuar",
    nameRequired: "Informe seu nome.",
    birthDateInvalid: "Use o formato DD/MM/AAAA.",
    birthDateRequired: "Informe sua data de nascimento.",
  },
  consents: {
    title: "Termos e privacidade",
    subtitle: "Para usar o MoveUp, leia e aceite os textos abaixo.",
    accept: "Li e aceito",
    submit: "Continuar",
    draftNotice:
      "Versão de rascunho: os textos definitivos chegam com a revisão jurídica antes do lançamento.",
    labels: {
      terms: "Termos de uso",
      privacy: "Política de privacidade",
      health_data:
        "Uso dos meus dados de saúde (anamnese, dores e treinos) pelo meu personal para montar e acompanhar meus treinos",
      photos: "Uso das minhas fotos de evolução",
    } satisfies Record<ConsentKind, string>,
    mustAcceptAll: "Marque todos os itens para continuar.",
  },
  guardian: {
    title: "Autorização do responsável",
    subtitle:
      "Como você tem menos de 18 anos, um dos seus pais ou responsável legal precisa autorizar o uso dos seus dados no MoveUp.",
    name: "Nome do responsável",
    email: "E-mail do responsável",
    relationshipLabel: "Parentesco",
    relationshipRequired: "Escolha o parentesco.",
    relationships: {
      mother: "Mãe",
      father: "Pai",
      legal_guardian: "Responsável legal",
      other: "Outro",
    } satisfies Record<GuardianRelationship, string>,
    declaration:
      "Declaro que sou o responsável e autorizo o uso dos dados de saúde e de treino deste aluno no MoveUp.",
    submit: "Enviar autorização",
    nameRequired: "Informe o nome do responsável.",
    emailInvalid: "Confira o e-mail do responsável.",
    mustDeclare: "O responsável precisa confirmar a autorização.",
  },
} as const;
