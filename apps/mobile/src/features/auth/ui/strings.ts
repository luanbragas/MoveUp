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
    title: "Quem\nautoriza?",
    subtitle: "Menores de 18 anos precisam da autorização de mãe, pai ou responsável.",
    name: "Nome do responsável",
    relationshipLabel: "Parentesco",
    relationshipRequired: "Escolha o parentesco.",
    relationships: {
      mother: "Mãe",
      father: "Pai",
      legal_guardian: "Responsável",
      other: "Outro",
    } satisfies Record<GuardianRelationship, string>,
    howItWorks: "Você manda um link pelo WhatsApp e a pessoa autoriza pelo celular dela.",
    submit: "Enviar pedido",
    nameRequired: "Informe o nome do responsável.",
    declinedBanner: (name: string) =>
      `${name} não autorizou. Converse com essa pessoa e peça de novo, ou indique outra.`,
    waitingTitle: (firstName: string) =>
      firstName.length <= 9 ? `Falta\n${firstName}.` : "Quase\nlá.",
    waitingLead: (name: string) =>
      `Mande o link para ${name}. Assim que a autorização chegar, seu app libera sozinho.`,
    waitingSince: (time: string, until: string) => `Pedido às ${time} · o link vale até ${until}.`,
    expired: "O link venceu. Mande um novo.",
    resend: "Mandar o link de novo",
    wrongPerson: "Pessoa errada?",
    changeGuardian: "Trocar responsável",
    back: "Voltar",
    shareMessage: (name: string, url: string) =>
      `Oi, ${name}! Preciso da sua autorização para usar o MoveUp, o app dos meus treinos. É rapidinho: ${url}`,
  },
} as const;
