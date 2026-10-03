// Textos do planejado no app do aluno (pt-BR), prontos para i18n.
export const strings = {
  today: {
    label: "Hoje",
    next: "Próximo",
    rest: "Hoje é descanso",
    restNext: (day: string, name: string) => `${day}: ${name}`,
    open: "Ver treino",
    meta: (exercises: number, minutes: number | null) =>
      [
        exercises === 1 ? "1 exercício" : `${String(exercises)} exercícios`,
        minutes === null ? null : `~${String(minutes)} min`,
      ]
        .filter((part) => part !== null)
        .join(" · "),
    perWeek: (n: number) => `meta: ${String(n)}x por semana`,
    week: (current: number, total: number) => `Semana ${String(current)} de ${String(total)}`,
    syncing: "Atualizando seus treinos…",
    offline: "Sem internet. Mostrando os treinos salvos no celular.",
    days: ["Domingo", "Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado"],
    otherWorkouts: "Treinos do programa",
  },
  preview: {
    back: "Voltar",
    block: (n: number) => `Bloco ${String(n)}`,
    howTo: "Como fazer",
    video: "Ver vídeo",
    notFound: "Este treino não está mais no seu programa.",
    soon: "Registrar as séries chega na próxima atualização do app.",
  },
} as const;
