import type { PlannedInput } from "../../execution";
import type { Workout } from "../domain/workout";

/** Treino do programa (do GET) vira o planejado do presencial: o personal registra pelo aluno. */
export function presencialInputOf(workout: Workout, linkId: string): PlannedInput {
  return {
    linkId,
    programId: workout.programId,
    workoutId: workout.id,
    versionId: workout.versionId,
    name: workout.draft.name,
    blocks: workout.draft.blocks.map((b) => ({
      method: b.method,
      preset: b.preset,
      rounds: b.rounds,
      workSeconds: b.workSeconds,
      restSeconds: b.restSeconds,
      restBetweenRounds: b.restBetweenRounds,
      durationSeconds: b.durationSeconds,
      name: b.name,
      exercises: b.exercises.map((e) => ({
        exerciseId: e.exerciseId,
        name: e.exerciseName,
        trackingType: e.trackingType,
        primaryMuscle: e.primaryMuscle,
        secondaryMuscles: [],
        restSeconds: e.restSeconds,
        notes: e.notes,
        sets: e.sets.map((s) => ({
          type: s.type,
          repsMin: s.repsMin,
          repsMax: s.repsMax,
          loadKg: s.loadKg,
          durationSeconds: s.durationSeconds,
          distanceM: s.distanceM,
          restSeconds: s.restSeconds,
        })),
      })),
    })),
  };
}
