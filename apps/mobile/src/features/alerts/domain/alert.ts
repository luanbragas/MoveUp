// Central de atenção do profissional, no formato do app (não o DTO da API).

export const ALERT_TYPES = [
  "pain_reported",
  "high_effort",
  "new_feedback",
  "session_edited",
  "inactive",
  "low_adherence",
  "clearance_pending",
] as const;
export type AlertType = (typeof ALERT_TYPES)[number];
export type Severity = "info" | "warning" | "urgent";
export type AlertStatus = "open" | "snoozed" | "resolved";

export interface Alert {
  readonly id: string;
  readonly type: AlertType;
  readonly severity: Severity;
  readonly status: AlertStatus;
  readonly clientId: string;
  /** Vínculo atual (para abrir a tela do aluno); nulo se o vínculo não existe mais. */
  readonly linkId: string | null;
  readonly clientName: string | null;
  /** Só números que explicam o alerta (dias, %, esforço, contagem). */
  readonly facts: Readonly<Record<string, number>>;
  readonly createdAt: Date;
  readonly snoozedUntil: Date | null;
}

export interface AlertPage {
  readonly items: readonly Alert[];
  readonly next: string | null;
  readonly openCount: number;
}

export interface AlertSetting {
  readonly type: AlertType;
  readonly enabled: boolean;
  readonly push: boolean;
  /** Dias sem treinar, esforço ou % de adesão; nulo nos tipos sem limite. */
  readonly threshold: number | null;
}

/** Faixa do limite de cada tipo (a mesma do servidor). */
export const THRESHOLD_RANGE: Partial<Record<AlertType, readonly [number, number]>> = {
  inactive: [2, 60],
  high_effort: [5, 10],
  low_adherence: [10, 100],
};

const SEVERITY_ORDER: Record<Severity, number> = { urgent: 0, warning: 1, info: 2 };

/** Abertos: urgentes primeiro, depois os mais novos (o servidor já manda do mais novo). */
export function byUrgency(alerts: readonly Alert[]): readonly Alert[] {
  return [...alerts].sort(
    (a, b) =>
      SEVERITY_ORDER[a.severity] - SEVERITY_ORDER[b.severity] ||
      b.createdAt.getTime() - a.createdAt.getTime(),
  );
}

export function clampThreshold(type: AlertType, value: number): number {
  const range = THRESHOLD_RANGE[type];
  return range === undefined ? value : Math.min(range[1], Math.max(range[0], Math.round(value)));
}
