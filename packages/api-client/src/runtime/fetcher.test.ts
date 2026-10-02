import { afterEach, describe, expect, it } from "vitest";
import { getMe, schemas } from "../index";
import { configureApiClient, resetApiClient } from "./fetcher";

interface Call {
  readonly url: string;
  readonly headers: Headers;
  readonly method: string | undefined;
}

function fakeFetch(response: Response): { calls: Call[]; fetch: typeof fetch } {
  const calls: Call[] = [];
  const fn: typeof fetch = (input, init) => {
    calls.push({
      url: input instanceof Request ? input.url : String(input),
      headers: new Headers(init?.headers),
      method: init?.method,
    });
    return Promise.resolve(response);
  };
  return { calls, fetch: fn };
}

const me = {
  id: "0192f5c4-1b2a-7c3d-8e4f-5a6b7c8d9e0f",
  name: "Usuária Fictícia",
  email: "ficticia@example.test",
  locale: "pt-BR",
  timezone: "America/Sao_Paulo",
  weightUnit: "kg",
  lengthUnit: "cm",
  professional: true,
};

describe("apiFetch", () => {
  afterEach(() => {
    resetApiClient();
  });

  it("usa a URL base e envia o token", async () => {
    const fake = fakeFetch(Response.json(me));
    configureApiClient({
      baseUrl: "https://api.example.test",
      getToken: () => Promise.resolve("token-123"),
      fetch: fake.fetch,
    });

    const response = await getMe();

    expect(fake.calls[0]?.url).toBe("https://api.example.test/v1/me");
    expect(fake.calls[0]?.method).toBe("GET");
    expect(fake.calls[0]?.headers.get("Authorization")).toBe("Bearer token-123");
    expect(response.status).toBe(200);
    expect(schemas.GetMe200Response.parse(response.data)).toEqual(me);
  });

  it("sem sessão não envia Authorization", async () => {
    const fake = fakeFetch(Response.json(me));
    configureApiClient({
      baseUrl: "https://api.example.test",
      getToken: () => Promise.resolve(null),
      fetch: fake.fetch,
    });

    await getMe();

    expect(fake.calls[0]?.headers.has("Authorization")).toBe(false);
  });

  it("entrega o ProblemDetail do erro para validação", async () => {
    const problem = {
      type: "https://api.moveup.com.br/problems/account-not-registered",
      title: "Conta não cadastrada",
      status: 404,
      detail: "Conta não cadastrada.",
      code: "account-not-registered",
      traceId: "4bf92f3577b34da6a3ce929d0e0e4736",
    };
    const fake = fakeFetch(
      new Response(JSON.stringify(problem), {
        status: 404,
        headers: { "content-type": "application/problem+json" },
      }),
    );
    configureApiClient({
      baseUrl: "https://api.example.test",
      getToken: () => Promise.resolve("t"),
      fetch: fake.fetch,
    });

    const response = await getMe();

    expect(response.status).toBe(404);
    expect(schemas.GetMe404Response.parse(response.data).code).toBe("account-not-registered");
  });

  it("resposta fora do contrato não passa no schema", async () => {
    const fake = fakeFetch(Response.json({ ...me, weightUnit: "arroba" }));
    configureApiClient({
      baseUrl: "https://api.example.test",
      getToken: () => Promise.resolve("t"),
      fetch: fake.fetch,
    });

    const response = await getMe();

    expect(schemas.GetMe200Response.safeParse(response.data).success).toBe(false);
  });

  it("falha claro se não foi configurado", async () => {
    await expect(getMe()).rejects.toThrow("api-client não configurado");
  });
});
