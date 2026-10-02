import * as SecureStore from "expo-secure-store";

// Armazenamento da sessão do Firebase no expo-secure-store (Keychain/Keystore):
// o token nunca vai para o AsyncStorage (FRONTEND-PATTERN, seção 11).
// O Secure Store só aceita chaves [A-Za-z0-9._-] e recomenda valores pequenos,
// então a chave é sanitizada e o valor é gravado em pedaços.

const CHUNK_SIZE = 1800;
const PREFIX = "moveup.auth.";

export interface KeyValueStore {
  getItemAsync(key: string): Promise<string | null>;
  setItemAsync(key: string, value: string): Promise<void>;
  deleteItemAsync(key: string): Promise<void>;
}

/** Interface que o Firebase espera de um storage de React Native. */
export interface AsyncStorageLike {
  getItem(key: string): Promise<string | null>;
  setItem(key: string, value: string): Promise<void>;
  removeItem(key: string): Promise<void>;
}

function safeKey(key: string): string {
  return PREFIX + key.replace(/[^A-Za-z0-9._-]/g, "_");
}

export function createChunkedStorage(store: KeyValueStore = SecureStore): AsyncStorageLike {
  async function chunkCount(base: string): Promise<number> {
    const raw = await store.getItemAsync(`${base}.n`);
    const count = raw === null ? 0 : Number.parseInt(raw, 10);
    return Number.isFinite(count) && count > 0 ? count : 0;
  }

  async function removeChunks(base: string): Promise<void> {
    const count = await chunkCount(base);
    for (let i = 0; i < count; i++) {
      await store.deleteItemAsync(`${base}.${String(i)}`);
    }
    await store.deleteItemAsync(`${base}.n`);
  }

  return {
    async getItem(key) {
      const base = safeKey(key);
      const count = await chunkCount(base);
      if (count === 0) {
        return null;
      }
      const parts: string[] = [];
      for (let i = 0; i < count; i++) {
        const part = await store.getItemAsync(`${base}.${String(i)}`);
        if (part === null) {
          return null; // gravação incompleta: trata como sem sessão
        }
        parts.push(part);
      }
      return parts.join("");
    },
    async setItem(key, value) {
      const base = safeKey(key);
      await removeChunks(base);
      const count = Math.max(1, Math.ceil(value.length / CHUNK_SIZE));
      for (let i = 0; i < count; i++) {
        await store.setItemAsync(
          `${base}.${String(i)}`,
          value.slice(i * CHUNK_SIZE, (i + 1) * CHUNK_SIZE),
        );
      }
      await store.setItemAsync(`${base}.n`, String(count));
    },
    async removeItem(key) {
      await removeChunks(safeKey(key));
    },
  };
}
