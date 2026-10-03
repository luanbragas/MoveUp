import { localDatabase } from "../../../shared/lib/local-db";
import type { SessionStore } from "../domain/ports";
import type { Session } from "../domain/session";

const finished = (s: Session) => s.status === "completed" || s.status === "partial";
const newestFirst = (a: Session, b: Session) => b.startedAt.localeCompare(a.startedAt);

/** Mesmo comportamento do SQLite, em memória (testes e web). */
export function createMemorySessionStore(): SessionStore {
  const rows = new Map<string, Session>();
  return {
    save: (session) =>
      Promise.resolve().then(() => {
        rows.set(session.id, session);
      }),
    get: (id) => Promise.resolve(rows.get(id) ?? null),
    active: () =>
      Promise.resolve(
        [...rows.values()].filter((s) => s.status === "in_progress").sort(newestFirst)[0] ?? null,
      ),
    history: () => Promise.resolve([...rows.values()].filter(finished).sort(newestFirst)),
    previousOf: (session) =>
      Promise.resolve(
        [...rows.values()]
          .filter(
            (s) =>
              finished(s) &&
              s.id !== session.id &&
              s.workoutId === session.workoutId &&
              s.startedAt < session.startedAt,
          )
          .sort(newestFirst)[0] ?? null,
      ),
    pending: () => Promise.resolve([...rows.values()].filter((s) => s.syncStatus === "pending")),
    markSynced: (sent) =>
      Promise.resolve().then(() => {
        for (const { id, clientUpdatedAt } of sent) {
          const row = rows.get(id);
          if (row === undefined) {
            continue;
          }
          if (row.clientUpdatedAt === clientUpdatedAt) {
            rows.set(id, { ...row, syncStatus: "synced" });
          }
        }
      }),
    clear: () =>
      Promise.resolve().then(() => {
        rows.clear();
      }),
  };
}

const parse = (row: { data: string; sync_status: string }): Session => {
  // sessões gravadas antes do resultado de bloco não têm o campo
  const stored = JSON.parse(row.data) as Omit<Session, "blockResults"> & {
    blockResults?: Session["blockResults"];
  };
  return {
    ...stored,
    blockResults: stored.blockResults ?? [],
    syncStatus: row.sync_status === "synced" ? "synced" : "pending",
  };
};

/** Sessões no SQLite: uma linha por sessão, com o objeto inteiro em JSON. */
export function createSqliteSessionStore(): SessionStore {
  return {
    async save(session) {
      const db = await localDatabase();
      await db.runAsync(
        `insert into performed_session
           (id, status, workout_id, started_at, client_updated_at, sync_status, data)
         values (?, ?, ?, ?, ?, ?, ?)
         on conflict (id) do update set status = excluded.status,
           client_updated_at = excluded.client_updated_at,
           sync_status = excluded.sync_status, data = excluded.data`,
        session.id,
        session.status,
        session.workoutId,
        session.startedAt,
        session.clientUpdatedAt,
        session.syncStatus,
        JSON.stringify(session),
      );
    },
    async get(id) {
      const db = await localDatabase();
      const row = await db.getFirstAsync<{ data: string; sync_status: string }>(
        "select data, sync_status from performed_session where id = ?",
        id,
      );
      return row === null ? null : parse(row);
    },
    async active() {
      const db = await localDatabase();
      const row = await db.getFirstAsync<{ data: string; sync_status: string }>(
        "select data, sync_status from performed_session where status = 'in_progress' order by started_at desc limit 1",
      );
      return row === null ? null : parse(row);
    },
    async history() {
      const db = await localDatabase();
      const rows = await db.getAllAsync<{ data: string; sync_status: string }>(
        "select data, sync_status from performed_session where status in ('completed', 'partial') order by started_at desc limit 200",
      );
      return rows.map(parse);
    },
    async previousOf(session) {
      if (session.workoutId === null) {
        return null;
      }
      const db = await localDatabase();
      const row = await db.getFirstAsync<{ data: string; sync_status: string }>(
        `select data, sync_status from performed_session
         where status in ('completed', 'partial') and workout_id = ? and id <> ? and started_at < ?
         order by started_at desc limit 1`,
        session.workoutId,
        session.id,
        session.startedAt,
      );
      return row === null ? null : parse(row);
    },
    async pending() {
      const db = await localDatabase();
      const rows = await db.getAllAsync<{ data: string; sync_status: string }>(
        "select data, sync_status from performed_session where sync_status = 'pending' order by started_at limit 50",
      );
      return rows.map(parse);
    },
    async markSynced(sent) {
      const db = await localDatabase();
      await db.withTransactionAsync(async () => {
        for (const { id, clientUpdatedAt } of sent) {
          // editada durante o envio: continua pendente para ir de novo
          await db.runAsync(
            "update performed_session set sync_status = 'synced' where id = ? and client_updated_at = ?",
            id,
            clientUpdatedAt,
          );
        }
      });
    },
    async clear() {
      const db = await localDatabase();
      await db.execAsync("delete from performed_session;");
    },
  };
}
