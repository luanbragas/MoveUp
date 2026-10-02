import { parseEnv } from "./env";

const valid = {
  EXPO_PUBLIC_API_URL: "https://api.moveup.com.br/",
  EXPO_PUBLIC_FIREBASE_API_KEY: "chave-de-teste-123",
  EXPO_PUBLIC_FIREBASE_PROJECT_ID: "moveup-teste",
  EXPO_PUBLIC_FIREBASE_APP_ID: "1:123:android:abc",
};

describe("parseEnv", () => {
  it("aceita URL http(s), tira a barra do fim e monta o authDomain", () => {
    const env = parseEnv(valid);

    expect(env.apiUrl).toBe("https://api.moveup.com.br");
    expect(env.firebase.authDomain).toBe("moveup-teste.firebaseapp.com");
  });

  it("app não sobe sem a URL da API ou com URL inválida", () => {
    expect(() => parseEnv({ ...valid, EXPO_PUBLIC_API_URL: undefined })).toThrow(
      "EXPO_PUBLIC_API_URL",
    );
    expect(() => parseEnv({ ...valid, EXPO_PUBLIC_API_URL: "api.moveup" })).toThrow(
      "EXPO_PUBLIC_API_URL",
    );
    expect(() => parseEnv({ ...valid, EXPO_PUBLIC_API_URL: "ftp://api.moveup.com.br" })).toThrow(
      "EXPO_PUBLIC_API_URL",
    );
  });

  it("app não sobe sem a configuração do Firebase", () => {
    expect(() => parseEnv({ ...valid, EXPO_PUBLIC_FIREBASE_API_KEY: "" })).toThrow(
      "EXPO_PUBLIC_FIREBASE_API_KEY",
    );
    expect(() => parseEnv({ ...valid, EXPO_PUBLIC_FIREBASE_PROJECT_ID: "Projeto Errado" })).toThrow(
      "EXPO_PUBLIC_FIREBASE_PROJECT_ID",
    );
  });
});
