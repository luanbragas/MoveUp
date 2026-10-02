import { parseEnv } from "./env";

describe("parseEnv", () => {
  it("aceita URL http(s) e tira a barra do fim", () => {
    expect(parseEnv({ EXPO_PUBLIC_API_URL: "https://api.moveup.com.br/" })).toEqual({
      apiUrl: "https://api.moveup.com.br",
    });
  });

  it("app não sobe sem a URL da API ou com URL inválida", () => {
    expect(() => parseEnv({})).toThrow("EXPO_PUBLIC_API_URL");
    expect(() => parseEnv({ EXPO_PUBLIC_API_URL: "api.moveup" })).toThrow("EXPO_PUBLIC_API_URL");
    expect(() => parseEnv({ EXPO_PUBLIC_API_URL: "ftp://api.moveup.com.br" })).toThrow(
      "EXPO_PUBLIC_API_URL",
    );
  });
});
