import { localDatabase as database } from "../../../../shared/lib/local-db";
import type { ExerciseInfo, PlannedProgram, PlannedWorkout } from "../../domain/planned";
import type { PlannedStore } from "../../domain/ports";

// Planejado no aparelho (ARQUITETURA 4). Cada linha guarda o objeto inteiro em JSON: o app sempre
// lê o programa todo, e a forma acompanha o contrato sem migração por coluna. Ao sair da conta,
// clear() apaga tudo. As tabelas vêm de shared/lib/local-db.

export function createSqliteStore(): PlannedStore {
  return {
    async cursor() {
      const db = await database();
      const row = await db.getFirstAsync<{ value: string | null }>(
        "select value from sync_state where key = 'planned_cursor'",
      );
      return row?.value ?? null;
    },

    async apply(changes) {
      const db = await database();
      await db.withTransactionAsync(async () => {
        for (const { program, deleted, workouts } of changes.programs) {
          await db.runAsync("delete from planned_workout where program_id = ?", program.id);
          if (deleted) {
            await db.runAsync("delete from planned_program where id = ?", program.id);
            continue;
          }
          await db.runAsync(
            `insert into planned_program (id, arrived, data) values (?, ?, ?)
             on conflict (id) do update set arrived = excluded.arrived, data = excluded.data`,
            program.id,
            Date.now(),
            JSON.stringify(program),
          );
          for (const workout of workouts) {
            await db.runAsync(
              `insert into planned_workout (id, program_id, data) values (?, ?, ?)
               on conflict (id) do update set program_id = excluded.program_id, data = excluded.data`,
              workout.id,
              program.id,
              JSON.stringify(workout),
            );
          }
        }
        for (const exercise of changes.exercises) {
          await db.runAsync(
            `insert into exercise_info (id, data) values (?, ?)
             on conflict (id) do update set data = excluded.data`,
            exercise.id,
            JSON.stringify(exercise),
          );
        }
        await db.runAsync(
          `insert into sync_state (key, value) values ('planned_cursor', ?)
           on conflict (key) do update set value = excluded.value`,
          changes.cursor,
        );
      });
    },

    async snapshot() {
      const db = await database();
      const programRow = await db.getFirstAsync<{ data: string }>(
        "select data from planned_program order by arrived desc limit 1",
      );
      const program = programRow === null ? null : (JSON.parse(programRow.data) as PlannedProgram);
      const workouts =
        program === null
          ? []
          : (
              await db.getAllAsync<{ data: string }>(
                "select data from planned_workout where program_id = ?",
                program.id,
              )
            )
              .map((r) => JSON.parse(r.data) as PlannedWorkout)
              .sort((a, b) => a.position - b.position);
      const exercises = new Map<string, ExerciseInfo>();
      for (const r of await db.getAllAsync<{ data: string }>("select data from exercise_info")) {
        const info = JSON.parse(r.data) as ExerciseInfo;
        exercises.set(info.id, info);
      }
      return { program, workouts, exercises };
    },

    async clear() {
      const db = await database();
      await db.execAsync(
        "delete from planned_workout; delete from planned_program; delete from exercise_info; delete from sync_state;",
      );
    },
  };
}
