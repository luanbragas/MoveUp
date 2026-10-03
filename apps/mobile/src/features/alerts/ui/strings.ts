// Textos da central de atenção (pt-BR), num lugar só e prontos para i18n.
import type { AlertType } from "../domain/alert";

const plural = (n: number, one: string, many: string) => `${String(n)} ${n === 1 ? one : many}`;

export const strings = {
  list: {
    title: "Atenção",
    settings: "Configurar alertas",
    filter: "Mostrar",
    open: (n: number) => (n > 0 ? `Abertos · ${String(n)}` : "Abertos"),
    snoozed: "Adiados",
    resolved: "Resolvidos",
    urgent: "Urgente",
    rest: "Para ver",
    emptyOpenTitle: "Tudo em dia por aqui",
    emptyOpenText:
      "Quando um aluno relatar dor, sumir ou mandar recado, aparece aqui primeiro, do mais urgente para o menos.",
    emptySnoozedTitle: "Nada adiado",
    emptySnoozedText: "O que você adiar volta para os abertos no dia marcado.",
    emptyResolvedTitle: "Nada resolvido ainda",
    emptyResolvedText: "Os alertas resolvidos ficam aqui para consulta.",
    more: "Ver mais",
    retry: "Tentar de novo",
    error: "Não conseguimos carregar os alertas.",
  },
  card: {
    resolve: "Resolver",
    snooze: "Adiar",
    openClient: (name: string) => `Abrir ${name}`,
    unknownClient: "Aluno",
    snoozedUntil: (date: string) => `Volta em ${date}`,
  },
  snooze: {
    title: "Adiar alerta",
    subtitle: "Ele some da lista e volta se continuar valendo.",
    days: (n: number) => (n === 1 ? "Amanhã" : `Em ${String(n)} dias`),
  },
  /** Título e detalhe de cada tipo, a partir dos números do alerta. */
  describe: (type: AlertType, facts: Readonly<Record<string, number>>) => {
    const n = (key: string) => facts[key] ?? 0;
    switch (type) {
      case "pain_reported":
        return {
          title: "Relatou dor no treino",
          detail: "Veja o feedback antes do próximo treino.",
        };
      case "high_effort":
        return {
          title: `Esforço ${String(n("effort"))} de 10`,
          detail: "O treino pesou mais que o normal.",
        };
      case "new_feedback":
        return { title: "Comentou o treino", detail: "Tem um recado no feedback." };
      case "session_edited":
        return {
          title: "Corrigiu um treino feito",
          detail: "As séries foram ajustadas depois de finalizar.",
        };
      case "inactive":
        return {
          title: `${plural(n("days"), "dia", "dias")} sem treinar`,
          detail: "Um toque agora costuma trazer o aluno de volta.",
        };
      case "low_adherence":
        return {
          title: `Adesão de ${String(n("percent"))}%`,
          detail: `${String(n("done"))} de ${plural(n("planned"), "treino", "treinos")} previstos em ${String(n("days"))} dias.`,
        };
    }
  },
  settings: {
    title: "Alertas",
    subtitle: "O que aparece na central e o que avisa no celular.",
    back: "Voltar",
    enabled: "Mostrar na central",
    push: "Avisar no celular",
    pushHint: "O aviso não mostra nome nem detalhe: só que um aluno precisa de atenção.",
    save: "Salvar",
    saved: "Alertas salvos.",
    less: "Diminuir",
    more: "Aumentar",
    types: {
      pain_reported: { label: "Dor relatada", unit: null },
      high_effort: { label: "Esforço alto", unit: "esforço a partir de" },
      new_feedback: { label: "Comentário no treino", unit: null },
      session_edited: { label: "Treino corrigido", unit: null },
      inactive: { label: "Aluno sem treinar", unit: "dias sem treinar" },
      low_adherence: { label: "Adesão baixa", unit: "% mínima em 14 dias" },
    } satisfies Record<AlertType, { label: string; unit: string | null }>,
  },
} as const;
