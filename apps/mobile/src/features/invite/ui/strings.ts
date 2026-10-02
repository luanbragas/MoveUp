// Textos da feature invite (pt-BR), num lugar só e prontos para i18n.
export const strings = {
  home: {
    title: "Início",
    error: "Não conseguimos carregar seu personal.",
    retry: "Tentar de novo",
    trainingSoon: "Seus treinos aparecem aqui em breve.",
  },
  link: {
    yourProfessional: "Seu personal",
    since: (date: string) => `Desde ${date}`,
    paused: "Seu personal pausou o acompanhamento por enquanto.",
    end: "Encerrar vínculo",
    confirmTitle: "Encerrar vínculo?",
    confirmMessage: (name: string) =>
      `${name} deixa de ver seus treinos novos. Seu histórico continua na sua conta.`,
    confirmBack: "Voltar",
  },
  code: {
    title: "Peça o link ao seu personal",
    subtitle: "Abra o link do convite ou digite o código de 8 letras e números.",
    label: "Código do convite",
    submit: "Ver convite",
    invalid: "O código tem 8 caracteres (letras e números, sem O, I, L, 0 e 1).",
  },
  preview: {
    wantsToCoach: (name: string) => `${name} quer ser seu personal`,
    organization: (name: string) => `Em ${name}`,
    expires: (date: string) => `Convite válido até ${date}.`,
    accept: "Aceitar",
    other: "Usar outro código",
  },
} as const;
