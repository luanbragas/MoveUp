import type { ConsentKind } from "../domain/me";
import type { GuardianRelationship } from "../domain/ports";
import type { AuthErrorCode } from "../domain/session";

// Textos da feature auth (pt-BR), num lugar só e prontos para i18n.
export const strings = {
  entry: {
    loading: "Carregando",
    errorTitle: "Não deu\npara abrir.",
    error: "Não conseguimos carregar sua conta. Confira a internet e tente de novo.",
    retry: "Tentar de novo",
    signOut: "Sair",
  },
  welcome: {
    title: "Treine\ncom seu\npersonal.",
    description: "Ele monta, você treina e ele acompanha. Funciona até sem sinal na academia.",
    professional: "Sou personal",
    client: "Tenho um convite",
    hasAccount: "Já tem conta?",
    signIn: "Entrar",
    // colagem do produto (ilustração: o leitor de tela lê só o resumo)
    collage: "Exemplo do app: série feita, descanso e mensagem do personal.",
    setDone: "Série feita",
    setValue: "60 kg",
    setReps: "· 10 reps",
    rest: "Descanso",
    restValue: "1:12",
    coachName: "Ana, sua personal",
    coachMessage: "Mandou bem, Bia!",
  },
  signIn: {
    titleSignIn: "Entrar",
    titleSignUp: "Criar\nconta",
    subtitle: "A mesma conta em qualquer celular.",
    email: "E-mail",
    password: "Senha",
    submitSignIn: "Entrar",
    submitSignUp: "Criar conta",
    noAccount: "Não tem conta?",
    toSignUp: "Criar conta",
    hasAccount: "Já tem conta?",
    toSignIn: "Entrar",
    forgot: "Esqueci",
    back: "Voltar",
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
    roleTitle: "Você\né...",
    roleSubtitle: "Escolha como vai usar o MoveUp.",
    roleLabel: "Você é",
    roleRequired: "Escolha se você é personal ou aluno.",
    professional: "Personal",
    client: "Aluno",
    nameTitle: "Como te\nchamam?",
    nameSubtitleProfessional: "É assim que seus alunos vão te ver.",
    nameSubtitleClient: "É assim que seu personal vai te ver.",
    name: "Nome completo",
    birthDate: "Data de nascimento",
    birthDateOptional: "Data de nascimento (opcional)",
    businessTitle: "Seu\nnegócio",
    businessSubtitle: "Opcional. Dá mais confiança no convite.",
    businessName: "Nome do negócio",
    registryNumber: "CREF",
    preview: "Seu aluno vai ver assim",
    skip: "Pular",
    next: "Continuar",
    back: "Voltar",
    signOut: "Sair",
    nameRequired: "Informe seu nome.",
    birthDateInvalid: "Use o formato DD/MM/AAAA.",
    birthDateRequired: "Informe sua data de nascimento.",
  },
  consents: {
    title: "Seus dados,\nsuas regras.",
    rows: {
      terms: { label: "Termos de uso", description: "Como o MoveUp funciona" },
      privacy: { label: "Privacidade", description: "O que guardamos e por quanto tempo" },
      health_data: {
        label: "Dados de saúde",
        description:
          "Autorizo meu personal a usar minha anamnese, dores e treinos para montar e acompanhar meus treinos.",
      },
      photos: { label: "Fotos de evolução", description: "Opcional · dá para ligar depois" },
    } satisfies Record<ConsentKind, { label: string; description: string }>,
    submit: "Aceitar e continuar",
    missing: (label: string) => `Falta aceitar: ${label.toLowerCase()}`,
    draftNotice:
      "Textos em rascunho: a versão definitiva chega com a revisão jurídica antes do lançamento.",
    back: "Voltar",
  },
  ready: {
    title: (firstName: string) =>
      firstName.length > 0 && firstName.length <= 9
        ? `Tudo\npronto,\n${firstName}.`
        : "Tudo\npronto.",
    description: "Agora convide seu primeiro aluno. Pelo WhatsApp ou QR Code, leva 1 minuto.",
    invite: "Convidar aluno",
    explore: "Explorar o app primeiro",
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
