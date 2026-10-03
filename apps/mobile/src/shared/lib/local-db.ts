import * as SQLite from "expo-sqlite";

// Banco local do app (ARQUITETURA 4: SQLite é a fonte da verdade no aparelho). As migrações ficam
// aqui, em ordem: nunca altere uma já publicada, só acrescente. Os dados são do próprio usuário,
// no aparelho dele; ao sair da conta, as features apagam o que é delas.

const MIGRATIONS: readonly string[] = [
  // 1: planejado baixado pelo GET /v1/sync
  `create table if not exists sync_state (key text primary key not null, value text);
   create table if not exists planned_program (
     id text primary key not null, arrived integer not null, data text not null);
   create table if not exists planned_workout (
     id text primary key not null, program_id text not null, data text not null);
   create index if not exists planned_workout_program on planned_workout (program_id);
   create table if not exists exercise_info (id text primary key not null, data text not null);`,
  // 2: treinos registrados (enviados pelo POST /v1/sync)
  `create table if not exists performed_session (
     id text primary key not null,
     status text not null,
     workout_id text,
     started_at text not null,
     client_updated_at text not null,
     sync_status text not null,
     data text not null);
   create index if not exists performed_session_status on performed_session (status, started_at);
   create index if not exists performed_session_sync on performed_session (sync_status);`,
];

let opening: Promise<SQLite.SQLiteDatabase> | null = null;

export function localDatabase(): Promise<SQLite.SQLiteDatabase> {
  opening ??= (async () => {
    const db = await SQLite.openDatabaseAsync("moveup.db");
    await db.execAsync("pragma journal_mode = wal;");
    const row = await db.getFirstAsync<{ user_version: number }>("pragma user_version");
    for (let v = row?.user_version ?? 0; v < MIGRATIONS.length; v += 1) {
      const step = MIGRATIONS[v];
      if (step !== undefined) {
        await db.withTransactionAsync(async () => {
          await db.execAsync(step);
          await db.execAsync(`pragma user_version = ${String(v + 1)}`);
        });
      }
    }
    return db;
  })();
  return opening;
}
