import type { z } from "zod";
import { ApiFailure } from "./app-error";
import { ProblemSchema } from "./problem-schema";

interface RawResponse {
  readonly status: number;
  readonly data: unknown;
}

/**
 * Chama uma função do api-client e valida a resposta na borda:
 * - 2xx: `schema.parse`; falha de parse é bug de contrato e vira `unexpected`;
 * - erro com ProblemDetail: vira `problem` com o `code` do backend;
 * - falha de rede (fetch rejeita com TypeError): vira `network`.
 * Nunca guarda nem registra o corpo da resposta (pode ter dado de saúde).
 */
export async function callApi<T>(
  request: () => Promise<RawResponse>,
  schema: z.ZodType<T>,
): Promise<T> {
  let response: RawResponse;
  try {
    response = await request();
  } catch (error) {
    if (error instanceof TypeError) {
      throw new ApiFailure({ kind: "network" });
    }
    throw error;
  }

  if (response.status >= 200 && response.status < 300) {
    const parsed = schema.safeParse(response.data);
    if (!parsed.success) {
      throw new ApiFailure({ kind: "unexpected" });
    }
    return parsed.data;
  }

  const problem = ProblemSchema.safeParse(response.data);
  if (!problem.success) {
    throw new ApiFailure({ kind: "unexpected" });
  }
  throw new ApiFailure({
    kind: "problem",
    code: problem.data.code,
    status: response.status,
    traceId: problem.data.traceId,
  });
}
