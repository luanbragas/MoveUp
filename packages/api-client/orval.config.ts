import { defineConfig } from "orval";

// Gera a partir do contrato commitado do backend (FRONTEND-PATTERN, seção 2):
// - client.ts: funções fetch tipadas, sem hooks (os hooks ficam nas features do app);
// - zod.ts: schemas Zod das respostas, para validar na borda (data/api).
// Nunca editar src/generated à mão: rode `pnpm api:generate`.
const input = { target: "../../backend/openapi/openapi.yaml" };

export default defineConfig({
  client: {
    input,
    output: {
      mode: "single",
      target: "src/generated/client.ts",
      client: "fetch",
      override: {
        mutator: { path: "src/runtime/fetcher.ts", name: "apiFetch" },
      },
    },
  },
  zod: {
    input,
    output: {
      mode: "single",
      target: "src/generated/zod.ts",
      client: "zod",
      // um schema por status (inclusive os de erro), para validar também o ProblemDetail
      override: { zod: { generateEachHttpStatus: true } },
    },
  },
});
