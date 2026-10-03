import { localDatabase } from "../../../shared/lib/local-db";
import type { StoredDraft, WorkoutDraftStore } from "../domain/ports";
import type { WorkoutDraft } from "../domain/workout";

/** Rascunhos em memória (web e testes). */
export function createMemoryDraftStore(): WorkoutDraftStore {
  const drafts = new Map<string, StoredDraft>();
  return {
    get: (workoutId) => Promise.resolve(drafts.get(workoutId) ?? null),
    save(workoutId, stored) {
      drafts.set(workoutId, stored);
      return Promise.resolve();
    },
    remove(workoutId) {
      drafts.delete(workoutId);
      return Promise.resolve();
    },
    clear() {
      drafts.clear();
      return Promise.resolve();
    },
  };
}

/** Rascunhos no SQLite do aparelho: uma linha por treino. */
export function createSqliteDraftStore(): WorkoutDraftStore {
  return {
    async get(workoutId) {
      const db = await localDatabase();
      const row = await db.getFirstAsync<{ base_revision: number; saved_at: string; data: string }>(
        "select base_revision, saved_at, data from workout_draft where workout_id = ?",
        workoutId,
      );
      return row === null
        ? null
        : {
            baseRevision: row.base_revision,
            savedAt: row.saved_at,
            draft: JSON.parse(row.data) as WorkoutDraft,
          };
    },
    async save(workoutId, stored) {
      const db = await localDatabase();
      await db.runAsync(
        `insert into workout_draft (workout_id, base_revision, saved_at, data) values (?, ?, ?, ?)
         on conflict (workout_id) do update set
           base_revision = excluded.base_revision, saved_at = excluded.saved_at, data = excluded.data`,
        workoutId,
        stored.baseRevision,
        stored.savedAt,
        JSON.stringify(stored.draft),
      );
    },
    async remove(workoutId) {
      const db = await localDatabase();
      await db.runAsync("delete from workout_draft where workout_id = ?", workoutId);
    },
    async clear() {
      const db = await localDatabase();
      await db.runAsync("delete from workout_draft");
    },
  };
}
