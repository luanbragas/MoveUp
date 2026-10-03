import type { Exercise, MuscleCode, NewExerciseInput } from "./exercise";

/** Biblioteca de exercícios (backend: módulo training). */
export interface ExercisesRepository {
  /** Texto vazio = todos em ordem alfabética. */
  search(query: string, muscle: MuscleCode | null): Promise<readonly Exercise[]>;
  create(input: NewExerciseInput): Promise<Exercise>;
  archive(exerciseId: string): Promise<void>;
}
