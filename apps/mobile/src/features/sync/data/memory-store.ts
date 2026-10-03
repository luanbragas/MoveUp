import type { ExerciseInfo, PlannedProgram, PlannedWorkout } from "../domain/planned";
import type { PlannedStore } from "../domain/ports";

/**
 * Mesmo comportamento do SQLite, em memória (testes e web). As regras de aplicar ficam em
 * {@link applyChanges} e valem para os dois.
 */
export interface PlannedTables {
  cursor: string | null;
  programs: Map<string, PlannedProgram>;
  /** Ordem de chegada: o programa ativo é o último que chegou vivo. */
  arrival: Map<string, number>;
  workouts: Map<string, PlannedWorkout>;
  exercises: Map<string, ExerciseInfo>;
}

let order = 0;

export function applyChanges(tables: PlannedTables, changes: Parameters<PlannedStore["apply"]>[0]) {
  for (const { program, deleted, workouts } of changes.programs) {
    for (const [id, workout] of tables.workouts) {
      if (workout.programId === program.id) {
        tables.workouts.delete(id);
      }
    }
    if (deleted) {
      tables.programs.delete(program.id);
      tables.arrival.delete(program.id);
      continue;
    }
    order += 1;
    tables.programs.set(program.id, program);
    tables.arrival.set(program.id, order);
    for (const workout of workouts) {
      tables.workouts.set(workout.id, workout);
    }
  }
  for (const exercise of changes.exercises) {
    tables.exercises.set(exercise.id, exercise);
  }
  tables.cursor = changes.cursor;
}

export function snapshotOf(tables: PlannedTables) {
  // programa ativo = o último que chegou vivo (o servidor arquiva os anteriores)
  const arrived = (id: string) => tables.arrival.get(id) ?? 0;
  const program =
    [...tables.programs.values()].sort((a, b) => arrived(b.id) - arrived(a.id))[0] ?? null;
  const workouts =
    program === null
      ? []
      : [...tables.workouts.values()]
          .filter((w) => w.programId === program.id)
          .sort((a, b) => a.position - b.position);
  return {
    program,
    workouts,
    exercises: new Map(tables.exercises),
  };
}

export function createMemoryStore(): PlannedStore {
  const tables: PlannedTables = {
    cursor: null,
    programs: new Map(),
    arrival: new Map(),
    workouts: new Map(),
    exercises: new Map(),
  };
  return {
    cursor: () => Promise.resolve(tables.cursor),
    apply: (changes) =>
      Promise.resolve().then(() => {
        applyChanges(tables, changes);
      }),
    snapshot: () => Promise.resolve(snapshotOf(tables)),
    clear: () =>
      Promise.resolve().then(() => {
        tables.cursor = null;
        tables.programs.clear();
        tables.arrival.clear();
        tables.workouts.clear();
        tables.exercises.clear();
      }),
  };
}
