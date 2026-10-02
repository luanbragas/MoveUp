// Erro único do app para tudo que vem da rede (FRONTEND-PATTERN, seção 9).
// A UI decide a mensagem pelo `code`, via catálogo; nunca exibe `detail` cru.
export type AppError =
  | {
      readonly kind: "problem";
      readonly code: string;
      readonly status: number;
      readonly traceId: string;
    }
  | { readonly kind: "network" }
  | { readonly kind: "unexpected" };

/** Exceção que carrega um AppError (o que o TanStack Query recebe como `error`). */
export class ApiFailure extends Error {
  readonly error: AppError;

  constructor(error: AppError) {
    super(error.kind === "problem" ? `api_problem:${error.code}` : `api_${error.kind}`);
    this.name = "ApiFailure";
    this.error = error;
  }
}

export function toAppError(error: unknown): AppError {
  return error instanceof ApiFailure ? error.error : { kind: "unexpected" };
}

/** 4xx não adianta repetir: a resposta vai ser a mesma. */
export function isClientError(error: unknown): boolean {
  const appError = toAppError(error);
  return appError.kind === "problem" && appError.status >= 400 && appError.status < 500;
}
