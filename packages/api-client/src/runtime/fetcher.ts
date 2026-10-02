// Função de transporte usada pelo cliente gerado pelo Orval (override.mutator).
// Escrita à mão; o resto de src/generated não.
//
// Devolve { data, status, headers } com `data` ainda não confiável: quem chama
// (data/api no app) valida com os schemas Zod gerados antes de usar.

export interface ApiClientConfig {
  /** Ex.: https://api.moveup.com.br (sem barra no fim). */
  readonly baseUrl: string;
  /** ID token atual do Firebase, ou null sem sessão. */
  readonly getToken: () => Promise<string | null>;
  /** Permite injetar fetch em testes. */
  readonly fetch?: typeof fetch;
}

export interface ApiResponse {
  readonly data: unknown;
  readonly status: number;
  readonly headers: Headers;
}

let config: ApiClientConfig | null = null;

export function configureApiClient(next: ApiClientConfig): void {
  config = next;
}

export function resetApiClient(): void {
  config = null;
}

async function readBody(response: Response): Promise<unknown> {
  if (response.status === 204) {
    return undefined;
  }
  const text = await response.text();
  if (text === "") {
    return undefined;
  }
  const contentType = response.headers.get("content-type") ?? "";
  if (contentType.includes("json")) {
    try {
      return JSON.parse(text) as unknown;
    } catch {
      return text;
    }
  }
  return text;
}

async function request(url: string, options: RequestInit): Promise<ApiResponse> {
  if (config === null) {
    throw new Error("api-client não configurado: chame configureApiClient() na inicialização");
  }
  const headers = new Headers(options.headers);
  headers.set("Accept", "application/json, application/problem+json");
  const token = await config.getToken();
  if (token !== null) {
    headers.set("Authorization", `Bearer ${token}`);
  }
  const doFetch = config.fetch ?? fetch;
  const response = await doFetch(`${config.baseUrl}${url}`, { ...options, headers });
  return { data: await readBody(response), status: response.status, headers: response.headers };
}

/**
 * Assinatura exigida pelo Orval. O tipo T é a união de respostas declarada no contrato; o conteúdo
 * real só é confiável depois do parse com Zod (FRONTEND-PATTERN, seção 7).
 */
export const apiFetch = async <T>(url: string, options: RequestInit): Promise<T> =>
  // Fronteira com o código gerado: o `as` só diz ao TypeScript a forma esperada.
  (await request(url, options)) as T;
