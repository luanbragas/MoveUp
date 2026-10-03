// Exercício da biblioteca, no formato do domínio do app (não o DTO da API).

/** Códigos dos grupos musculares (os mesmos do mapa muscular e da API). */
export const MUSCLE_CODES = [
  "chest",
  "delts",
  "traps",
  "lats",
  "lowerback",
  "biceps",
  "triceps",
  "forearms",
  "abs",
  "quads",
  "hamstrings",
  "glutes",
  "adductors",
  "abductors",
  "calves",
] as const;
export type MuscleCode = (typeof MUSCLE_CODES)[number];

export type Modality = "strength" | "cardio" | "conditioning" | "complementary";
/** O que o aluno registra em cada série. */
export type TrackingType = "reps_load" | "reps_only" | "time" | "distance_time";

export interface Exercise {
  readonly id: string;
  readonly name: string;
  readonly modality: Modality;
  readonly trackingType: TrackingType;
  readonly primaryMuscle: MuscleCode | null;
  readonly secondaryMuscles: readonly MuscleCode[];
  readonly equipment: string | null;
  readonly unilateral: boolean;
  readonly instructions: string | null;
  readonly mediaUrl: string | null;
  /** Próprio da organização do personal (pode arquivar); falso = biblioteca base. */
  readonly custom: boolean;
}

export interface NewExerciseInput {
  readonly name: string;
  readonly modality: Modality;
  readonly trackingType: TrackingType;
  readonly primaryMuscle: MuscleCode | null;
  readonly secondaryMuscles: readonly MuscleCode[];
  readonly equipment: string | null;
  readonly unilateral: boolean;
  readonly instructions: string | null;
  readonly mediaUrl: string | null;
}

export function isMuscleCode(value: string | null | undefined): value is MuscleCode {
  return MUSCLE_CODES.some((code) => code === value);
}

/** Níveis para o mapa muscular: principal = 2, secundários = 1. */
export function muscleLevels(exercise: Exercise): Partial<Record<MuscleCode, 1 | 2>> {
  const levels: Partial<Record<MuscleCode, 1 | 2>> = {};
  for (const muscle of exercise.secondaryMuscles) {
    levels[muscle] = 1;
  }
  if (exercise.primaryMuscle !== null) {
    levels[exercise.primaryMuscle] = 2;
  }
  return levels;
}
