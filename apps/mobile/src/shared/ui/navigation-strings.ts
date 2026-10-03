// Nomes das abas e textos das abas que ainda não têm feature (pt-BR, prontos para i18n).
export const navigationStrings = {
  client: {
    home: "Hoje",
    training: "Treino",
    history: "Histórico",
    progress: "Evolução",
    profile: "Perfil",
  },
  professional: {
    dashboard: "Painel",
    clients: "Alunos",
    training: "Treinos",
    alerts: "Atenção",
    settings: "Ajustes",
  },
  soon: {
    clientTraining: {
      title: "Treino",
      icon: "dumbbell",
      emptyTitle: "Seu treino vem aqui",
      description: "Quando seu personal montar o treino, ele aparece aqui, pronto para começar.",
    },
    clientHistory: {
      title: "Histórico",
      icon: "clock",
      emptyTitle: "Seu primeiro treino aparece aqui",
      description:
        "Cada treino terminado fica salvo com carga e esforço, para você comparar com a vez anterior.",
    },
    clientProgress: {
      title: "Evolução",
      icon: "trend",
      emptyTitle: "Comece pela primeira avaliação",
      description:
        "Medidas, peso e % de gordura de hoje viram o ponto de partida. Daqui a algumas semanas, sua evolução aparece aqui.",
    },
    clientProfile: {
      title: "Perfil",
      icon: "user",
      emptyTitle: "Seus dados e privacidade",
      description:
        "Unidades, notificações e o que você compartilha com o personal chegam aqui em breve.",
    },
    dashboard: {
      title: "Painel",
      icon: "chart",
      emptyTitle: "Sua carteira em um olhar",
      description:
        "Alunos ativos, adesão média e o mapa de treinos das últimas semanas aparecem aqui quando seus alunos começarem a treinar.",
    },
    alerts: {
      title: "Atenção",
      icon: "bell",
      emptyTitle: "Tudo em dia por aqui",
      description:
        "Quando um aluno relatar dor, sumir ou mandar recado, aparece aqui primeiro, do mais urgente para o menos.",
    },
    settings: {
      title: "Ajustes",
      icon: "sliders",
      emptyTitle: "Perfil, plano e privacidade",
      description: "Seus dados, o plano e as preferências do app chegam aqui em breve.",
    },
  },
} as const;
