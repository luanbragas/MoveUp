import { configureApiClient, resetApiClient } from "@moveup/api-client";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react-native";
import { http, HttpResponse } from "msw";
import { setupServer } from "msw/native";
import type { ReactNode } from "react";
import { RepositoriesProvider, createRepositories } from "../../../providers/repositories";
import { createFakeSession } from "../data/fakes/fake-session";
import { toAppError } from "../../../shared/lib/http";
import { useMe } from "./use-me";

// Usa o cliente gerado de verdade (packages/api-client) contra uma API simulada pelo MSW.
const API = "https://api.example.test";

const meDto = {
  id: "0192f5c4-1b2a-7c3d-8e4f-5a6b7c8d9e0f",
  name: "Personal Fictícia",
  email: "personal@example.test",
  locale: "pt-BR",
  timezone: "America/Sao_Paulo",
  weightUnit: "kg",
  lengthUnit: "cm",
  role: "professional",
  minor: false,
  missingConsents: ["terms"],
  guardianConsentRequired: false,
};

let receivedAuthorization: string | null = null;

const server = setupServer(
  http.get(`${API}/v1/me`, ({ request }) => {
    receivedAuthorization = request.headers.get("Authorization");
    return HttpResponse.json(meDto);
  }),
);

beforeAll(() => {
  server.listen({ onUnhandledRequest: "error" });
});
beforeEach(() => {
  receivedAuthorization = null;
  configureApiClient({ baseUrl: API, getToken: () => Promise.resolve("token-de-teste") });
});
afterEach(() => {
  server.resetHandlers();
  resetApiClient();
});
afterAll(() => {
  server.close();
});

function wrapper({ children }: { readonly children: ReactNode }) {
  // gcTime infinito: sem timers de coleta pendurados entre os testes
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: Number.POSITIVE_INFINITY } },
  });
  return (
    <QueryClientProvider client={queryClient}>
      <RepositoriesProvider repositories={createRepositories(createFakeSession())}>
        {children}
      </RepositoriesProvider>
    </QueryClientProvider>
  );
}

describe("useMe", () => {
  it("busca a conta com o token e entrega no formato do domínio", async () => {
    const { result } = await renderHook(() => useMe("uid-teste"), { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });
    expect(receivedAuthorization).toBe("Bearer token-de-teste");
    expect(result.current.data).toEqual({
      id: meDto.id,
      name: meDto.name,
      email: meDto.email,
      locale: "pt-BR",
      timezone: "America/Sao_Paulo",
      units: { weight: "kg", length: "cm" },
      role: "professional",
      isMinor: false,
      onboarding: {
        missingConsents: ["terms"],
        guardianConsentRequired: false,
        guardianRequest: null,
      },
    });
  });

  it("login sem cadastro chega como AppError account-not-registered", async () => {
    server.use(
      http.get(`${API}/v1/me`, () =>
        HttpResponse.json(
          {
            type: "https://api.moveup.com.br/problems/account-not-registered",
            title: "Conta não cadastrada",
            status: 404,
            detail: "Conta não cadastrada.",
            instance: "/v1/me",
            code: "account-not-registered",
            traceId: "4bf92f3577b34da6a3ce929d0e0e4736",
          },
          { status: 404, headers: { "Content-Type": "application/problem+json" } },
        ),
      ),
    );

    const { result } = await renderHook(() => useMe("uid-teste"), { wrapper });

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });
    expect(toAppError(result.current.error)).toEqual({
      kind: "problem",
      code: "account-not-registered",
      status: 404,
      traceId: "4bf92f3577b34da6a3ce929d0e0e4736",
    });
  });

  it("resposta fora do contrato não chega à UI", async () => {
    server.use(http.get(`${API}/v1/me`, () => HttpResponse.json({ ...meDto, id: "nao-e-uuid" })));

    const { result } = await renderHook(() => useMe("uid-teste"), { wrapper });

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });
    expect(toAppError(result.current.error)).toEqual({ kind: "unexpected" });
  });
});
