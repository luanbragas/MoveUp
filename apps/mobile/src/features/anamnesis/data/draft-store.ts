import { localDatabase } from "../../../shared/lib/local-db";
import type { Answers } from "../domain/anamnesis";
import type { AnamnesisDraftStore } from "../domain/ports";

const KEY = "anamnesis_draft";

/** Em memória (web e testes). */
export function createMemoryAnamnesisDraft(): AnamnesisDraftStore {
  let draft: Answers | null = null;
  return {
    get: () => Promise.resolve(draft),
    save(answers) {
      draft = answers;
      return Promise.resolve();
    },
    clear() {
      draft = null;
      return Promise.resolve();
    },
  };
}

/** No SQLite do aparelho (sai junto com o resto ao sair da conta). */
export function createSqliteAnamnesisDraft(): AnamnesisDraftStore {
  return {
    async get() {
      const db = await localDatabase();
      const row = await db.getFirstAsync<{ value: string }>(
        "select value from local_value where key = ?",
        KEY,
      );
      return row === null ? null : (JSON.parse(row.value) as Answers);
    },
    async save(answers) {
      const db = await localDatabase();
      await db.runAsync(
        `insert into local_value (key, value) values (?, ?)
         on conflict (key) do update set value = excluded.value`,
        KEY,
        JSON.stringify(answers),
      );
    },
    async clear() {
      const db = await localDatabase();
      await db.runAsync("delete from local_value where key = ?", KEY);
    },
  };
}
