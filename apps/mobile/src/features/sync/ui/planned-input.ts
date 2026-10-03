import type { PlannedInput } from "../../execution";
import type { ExerciseInfo, PlannedProgram, PlannedWorkout } from "../domain/planned";

/** O treino baixado vira o planejado que inicia a sessão (com nome e músculos de cada exercício). */
export function plannedInputOf(
  workout: PlannedWorkout,
  program: PlannedProgram,
  exercises: ReadonlyMap<string, ExerciseInfo>,
): PlannedInput {
  return {
    linkId: program.linkId,
    programId: program.id,
    workoutId: workout.id,
    versionId: workout.versionId,
    name: workout.name,
    blocks: workout.blocks.map((b) => ({
      method: b.method,
      preset: b.preset,
      rounds: b.rounds,
      workSeconds: b.workSeconds,
      restSeconds: b.restSeconds,
      restBetweenRounds: b.restBetweenRounds,
      durationSeconds: b.durationSeconds,
      name: b.name,
      exercises: b.exercises.map((e) => {
        const info = exercises.get(e.exerciseId);
        return {
          exerciseId: e.exerciseId,
          name: info?.name ?? "",
          trackingType: info?.trackingType ?? "reps_load",
          primaryMuscle: info?.primaryMuscle ?? null,
          secondaryMuscles: info?.secondaryMuscles ?? [],
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
        };
      }),
    })),
  };
}
