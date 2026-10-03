// Textos da feature invite (pt-BR), num lugar só e prontos para i18n.
export const strings = {
  home: {
    greeting: (firstName: string) =>
      firstName.length > 0 && firstName.length <= 9 ? `Bora,\n${firstName}.` : "Bora\ntreinar.",
    error: "Não conseguimos carregar seu personal.",
    retry: "Tentar de novo",
    emptyTitle: "Seu treino vem aqui",
    emptyText: (professional: string) =>
      `${professional} está montando seu primeiro treino. Você recebe um aviso assim que ele chegar.`,
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
    title: "Tem um\ncódigo?",
    subtitle: "8 letras e números que seu personal te mandou.",
    label: "Código do convite",
    fromLink: "Abriu pelo link? Ele já vem preenchido.",
    submit: "Ver convite",
    noCode: "Sem código?",
    askLink: "Peça o link ao seu personal",
    askLinkHelp:
      "Peça ao seu personal o link do convite pelo WhatsApp. Ao abrir o link, o código já vem preenchido.",
    invalid: "O código tem 8 caracteres (letras e números, sem O, I, L, 0 e 1).",
  },
  preview: {
    wantsToCoach: "quer ser seu\npersonal",
    organization: (name: string, date: string) => `${name} · vale até ${date}`,
    expires: (date: string) => `Vale até ${date}`,
    can: "O que seu personal pode fazer",
    abilities: [
      "Monta e ajusta seus treinos",
      "Vê os treinos que você fizer",
      "Lê sua anamnese e seus relatos de dor",
    ],
    accept: "Aceitar convite",
    other: "Não conheço essa pessoa",
    back: "Voltar",
  },
  expired: {
    title: "Esse convite\njá era.",
    text: "O código passou da validade ou já foi usado. Peça um novo ao seu personal: leva um minuto.",
    other: "Digitar outro código",
  },
  accepted: {
    title: (firstName: string) =>
      firstName.length > 0 && firstName.length <= 9
        ? `Você está\nno time,\n${firstName}.`
        : "Você está\nno time.",
    text: (professional: string) =>
      `${professional} agora acompanha seus treinos. O primeiro treino aparece no início assim que ficar pronto.`,
    go: "Ir para o início",
  },
} as const;
