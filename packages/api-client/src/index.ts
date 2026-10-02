// Ponto de entrada do cliente da API. O conteúdo de ./generated vem do Orval
// (pnpm api:generate) a partir de backend/openapi/openapi.yaml.
export * from "./generated/client";
export * as schemas from "./generated/zod";
export {
  configureApiClient,
  resetApiClient,
  type ApiClientConfig,
  type ApiResponse,
} from "./runtime/fetcher";
