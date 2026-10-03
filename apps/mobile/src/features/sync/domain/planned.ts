// O planejado do aluno guardado no aparelho (SQLite) e a regra do "treino de hoje".

export interface PlannedSet {
  readonly type: string;
  readonly repsMin: number | null;
  readonly repsMax: number | null;
  readonly loadKg: number | null;
  readonly durationSeconds: number | null;
  readonly distanceM: number | null;
  readonly targetRir: number | null;
  readonly restSeconds: number | null;
}

export interface PlannedExercise {
  readonly exerciseId: string;
  readonly restSeconds: number | null;
  readonly notes: string | null;
  readonly sets: readonly PlannedSet[];
}

export interface PlannedBlock {
  readonly name: string | null;
  readonly method: string;
  readonly preset: string | null;
  readonly rounds: number | null;
  readonly workSeconds: number | null;
  readonly restSeconds: number | null;
  readonly restBetweenRounds: number | null;
  readonly durationSeconds: number | null;
  readonly exercises: readonly PlannedExercise[];
}

export interface PlannedWorkout {
  readonly id: string;
  readonly programId: string;
  readonly name: string;
  readonly position: number;
  /** 0 = domingo; vazio na sequência. */
  readonly weekdays: readonly number[];
  readonly versionId: string;
  readonly versionNumber: number;
  readonly goal: string | null;
  readonly estimatedMinutes: number | null;
  readonly notes: string | null;
  readonly blocks: readonly PlannedBlock[];
}

export interface PlannedProgram {
  readonly id: string;
  readonly linkId: string;
  readonly name: string;
  readonly goal: string | null;
  readonly startsOn: string | null;
  readonly endsOn: string | null;
  readonly scheduleMode: "fixed_days" | "sequence";
  readonly weeklyTarget: number | null;
}

export interface ExerciseInfo {
  readonly id: string;
  readonly name: string;
  readonly trackingType: string;
  readonly primaryMuscle: string | null;
  readonly secondaryMuscles: readonly string[];
  readonly equipment: string | null;
  readonly instructions: string | null;
  readonly mediaUrl: string | null;
}

/** Uma resposta do GET /v1/sync, já no formato do app. */
export interface PlannedChanges {
  readonly cursor: string;
  readonly programs: readonly {
    readonly program: PlannedProgram;
    readonly deleted: boolean;
    readonly workouts: readonly PlannedWorkout[];
  }[];
  readonly exercises: readonly ExerciseInfo[];
}

/** O que o app mostra: o programa ativo (o mais recente) e os treinos dele. */
export interface PlannedSnapshot {
  readonly program: PlannedProgram | null;
  readonly workouts: readonly PlannedWorkout[];
  readonly exercises: ReadonlyMap<string, ExerciseInfo>;
}

export type Today =
  | { readonly kind: "none" }
  | { readonly kind: "workout"; readonly workout: PlannedWorkout }
  /** Dias fixos sem treino hoje: o próximo da semana. */
  | {
      readonly kind: "rest";
      readonly next: PlannedWorkout | null;
      readonly nextWeekday: number | null;
    }
  /** Sequência: o próximo da fila (até existir histórico, o primeiro). */
  | { readonly kind: "next"; readonly workout: PlannedWorkout };

/**
 * Treino de hoje. Dias fixos: o do dia da semana (se houver mais de um, o primeiro na ordem);
 * sem treino hoje, o próximo dia com treino. Sequência: o próximo da fila — a Fase 3 passa o
 * último feito; sem ele, o primeiro.
 */
export function todayOf(
  snapshot: PlannedSnapshot,
  date: Date,
  lastDoneWorkoutId: string | null = null,
): Today {
  const { program, workouts } = snapshot;
  if (program === null || workouts.length === 0) {
    return { kind: "none" };
  }
  const ordered = [...workouts].sort((a, b) => a.position - b.position);
  if (program.scheduleMode === "sequence") {
    const lastIndex = ordered.findIndex((w) => w.id === lastDoneWorkoutId);
    const next = ordered[(lastIndex + 1) % ordered.length] ?? ordered[0];
    return next === undefined ? { kind: "none" } : { kind: "next", workout: next };
  }
  const weekday = date.getDay();
  const todays = ordered.find((w) => w.weekdays.includes(weekday));
  if (todays !== undefined) {
    return { kind: "workout", workout: todays };
  }
  for (let offset = 1; offset <= 7; offset += 1) {
    const day = (weekday + offset) % 7;
    const next = ordered.find((w) => w.weekdays.includes(day));
    if (next !== undefined) {
      return { kind: "rest", next, nextWeekday: day };
    }
  }
  return { kind: "rest", next: null, nextWeekday: null };
}

/** Quantos exercícios o treino tem (para o card). */
export function exerciseCount(workout: PlannedWorkout): number {
  return workout.blocks.reduce((sum, b) => sum + b.exercises.length, 0);
}
