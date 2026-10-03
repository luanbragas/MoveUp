// Programa do aluno e modelos, no formato do domínio do app.

export type ScheduleMode = "fixed_days" | "sequence";

export interface ProgramWorkout {
  readonly id: string;
  readonly name: string;
  /** Ordem na agenda (A = 1). */
  readonly position: number;
  /** 0 = domingo; vazio no modo sequência. */
  readonly weekdays: readonly number[];
  readonly estimatedMinutes: number | null;
  readonly exercises: number;
}

export interface Program {
  readonly id: string;
  readonly linkId: string;
  readonly name: string;
  readonly goal: string | null;
  readonly startsOn: string | null;
  readonly endsOn: string | null;
  readonly scheduleMode: ScheduleMode;
  readonly weeklyTarget: number | null;
  readonly revision: number;
  readonly workouts: readonly ProgramWorkout[];
}

export interface ProgramInput {
  readonly name: string;
  readonly goal: string | null;
  readonly startsOn: string | null;
  readonly endsOn: string | null;
  readonly scheduleMode: ScheduleMode;
  readonly weeklyTarget: number | null;
}

export interface TemplateSummary {
  readonly id: string;
  readonly name: string;
  readonly exercises: number;
  readonly blocks: number;
  readonly estimatedMinutes: number | null;
  readonly updatedAt: Date;
}

/** Letra do treino na sequência: A, B, C… */
export function letterOf(position: number): string {
  return String.fromCharCode(64 + Math.min(Math.max(position, 1), 26));
}

const DAY_SHORT = ["dom", "seg", "ter", "qua", "qui", "sex", "sáb"] as const;

/** "seg e qui", "seg, qua e sex". */
export function weekdaysLabel(weekdays: readonly number[]): string {
  const names = [...weekdays].sort((a, b) => a - b).map((d) => DAY_SHORT[d] ?? "");
  if (names.length <= 1) {
    return names.join("");
  }
  return `${names.slice(0, -1).join(", ")} e ${names.at(-1) ?? ""}`;
}

/** Semana atual do programa ("Semana 3 de 8"), se tiver início e fim. */
export function programWeek(
  program: Program,
  today: Date,
): { current: number; total: number } | null {
  if (program.startsOn === null || program.endsOn === null) {
    return null;
  }
  const start = new Date(`${program.startsOn}T00:00:00`);
  const end = new Date(`${program.endsOn}T00:00:00`);
  const week = 7 * 24 * 60 * 60 * 1000;
  const total = Math.max(1, Math.ceil((end.getTime() - start.getTime() + 1) / week));
  const current = Math.min(
    total,
    Math.max(1, Math.floor((today.getTime() - start.getTime()) / week) + 1),
  );
  return { current, total };
}
