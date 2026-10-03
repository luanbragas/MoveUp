import * as SQLite from "expo-sqlite";
import type { ExerciseInfo, PlannedProgram, PlannedWorkout } from "../../domain/planned";
import type { PlannedStore } from "../../domain/ports";

// Fonte da verdade local do planejado (ARQUITETURA 4). Cada linha guarda o objeto inteiro em
// JSON: o app sempre lê o programa todo, e a forma acompanha o contrato sem migração por coluna.
// O dado é do próprio aluno, no aparelho dele; ao sair da conta, clear() apaga tudo.

const SCHEMA_VERSION = 1;

const MIGRATIONS = [
  `create table if not exists sync_state (key text primary key not null, value text);
   create table if not exists planned_program (
     id text primary key not null, arrived integer not null, data text not null);
   create table if not exists planned_workout (
     id text primary key not null, program_id text not null, data text not null);
   create index if not exists planned_workout_program on planned_workout (program_id);
   create table if not exists exercise_info (id text primary key not null, data text not null);`,
];

let opening: Promise<SQLite.SQLiteDatabase> | null = null;

async function database(): Promise<SQLite.SQLiteDatabase> {
  opening ??= (async () => {
    const db = await SQLite.openDatabaseAsync("moveup.db");
    await db.execAsync("pragma journal_mode = wal;");
    const row = await db.getFirstAsync<{ user_version: number }>("pragma user_version");
    const current = row?.user_version ?? 0;
    for (let v = current; v < SCHEMA_VERSION; v += 1) {
      const step = MIGRATIONS[v];
      if (step !== undefined) {
        await db.execAsync(step);
      }
    }
    await db.execAsync(`pragma user_version = ${String(SCHEMA_VERSION)}`);
    return db;
  })();
  return opening;
}

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
