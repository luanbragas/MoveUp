import { createChunkedStorage, type KeyValueStore } from "./secure-store-storage";

function memoryStore(): KeyValueStore & { readonly data: Map<string, string> } {
  const data = new Map<string, string>();
  return {
    data,
    getItemAsync: (key) => Promise.resolve(data.get(key) ?? null),
    setItemAsync: (key, value) => {
      if (!/^[A-Za-z0-9._-]+$/.test(key)) {
        return Promise.reject(new Error(`chave inválida para o Secure Store: ${key}`));
      }
      data.set(key, value);
      return Promise.resolve();
    },
    deleteItemAsync: (key) => {
      data.delete(key);
      return Promise.resolve();
    },
  };
}

describe("createChunkedStorage", () => {
  const firebaseKey = "firebase:authUser:abc123:[DEFAULT]";

  it("grava valor grande em pedaços com chave aceita pelo Secure Store", async () => {
    const store = memoryStore();
    const storage = createChunkedStorage(store);
    const big = "x".repeat(5000);

    await storage.setItem(firebaseKey, big);

    expect(await storage.getItem(firebaseKey)).toBe(big);
    expect([...store.data.keys()].every((k) => /^[A-Za-z0-9._-]+$/.test(k))).toBe(true);
    expect(store.data.size).toBe(4); // 3 pedaços + contador
  });

  it("regravar menor apaga os pedaços antigos", async () => {
    const store = memoryStore();
    const storage = createChunkedStorage(store);

    await storage.setItem(firebaseKey, "x".repeat(5000));
    await storage.setItem(firebaseKey, "pequeno");

    expect(await storage.getItem(firebaseKey)).toBe("pequeno");
    expect(store.data.size).toBe(2);
  });

  it("remover apaga tudo e leitura sem valor devolve null", async () => {
    const store = memoryStore();
    const storage = createChunkedStorage(store);
    await storage.setItem(firebaseKey, "sessao");

    await storage.removeItem(firebaseKey);

    expect(await storage.getItem(firebaseKey)).toBeNull();
    expect(store.data.size).toBe(0);
  });

  it("pedaço faltando conta como sem sessão", async () => {
    const store = memoryStore();
    const storage = createChunkedStorage(store);
    await storage.setItem(firebaseKey, "x".repeat(4000));
    const firstChunk = [...store.data.keys()].find((k) => k.endsWith(".0"));
    if (firstChunk !== undefined) {
      store.data.delete(firstChunk);
    }

    expect(await storage.getItem(firebaseKey)).toBeNull();
  });
});
