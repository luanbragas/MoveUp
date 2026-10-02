import { z } from "zod";

// Variáveis de ambiente do app, validadas na inicialização (FRONTEND-PATTERN, seção 7):
// o app não sobe com env inválido. Só variáveis EXPO_PUBLIC_* chegam ao bundle,
// então nada aqui pode ser segredo.
const EnvSchema = z.object({
  EXPO_PUBLIC_API_URL: z.url({ protocol: /^https?$/ }),
});

export interface Env {
  /** URL base da API, sem barra no fim. */
  readonly apiUrl: string;
}

export function parseEnv(source: Readonly<Record<string, string | undefined>>): Env {
  const parsed = EnvSchema.safeParse(source);
  if (!parsed.success) {
    throw new Error(
      `Variáveis de ambiente inválidas: ${parsed.error.issues.map((i) => i.path.join(".")).join(", ")}`,
    );
  }
  return { apiUrl: parsed.data.EXPO_PUBLIC_API_URL.replace(/\/+$/, "") };
}

export function loadEnv(): Env {
  // Acesso direto a process.env.EXPO_PUBLIC_*: é assim que o Expo injeta o valor no bundle.
  return parseEnv({ EXPO_PUBLIC_API_URL: process.env.EXPO_PUBLIC_API_URL });
}
