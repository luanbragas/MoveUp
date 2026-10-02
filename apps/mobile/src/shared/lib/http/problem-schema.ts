import { z } from "zod";

// ProblemDetail (RFC 9457) como o backend devolve: ver schema `Problem` em
// backend/openapi/openapi.yaml. Genérico para qualquer rota.
export const ProblemSchema = z.object({
  type: z.string(),
  title: z.string(),
  status: z.number().int(),
  detail: z.string(),
  instance: z.string().optional(),
  code: z.string().min(1),
  traceId: z.string().min(1),
  errors: z
    .array(z.object({ field: z.string(), code: z.string(), message: z.string() }))
    .optional(),
});

export type Problem = z.infer<typeof ProblemSchema>;
