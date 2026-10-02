import { z } from "zod";

// Variáveis de ambiente do app, validadas na inicialização (FRONTEND-PATTERN, seção 7):
// o app não sobe com env inválido. Só variáveis EXPO_PUBLIC_* chegam ao bundle,
// então nada aqui pode ser segredo de servidor.
const EnvSchema = z.object({
  EXPO_PUBLIC_API_URL: z.url({ protocol: /^https?$/ }),
  EXPO_PUBLIC_FIREBASE_API_KEY: z.string().min(10),
  EXPO_PUBLIC_FIREBASE_PROJECT_ID: z.string().regex(/^[a-z0-9-]{4,40}$/),
  EXPO_PUBLIC_FIREBASE_APP_ID: z.string().min(10),
});

export interface FirebaseConfig {
  readonly apiKey: string;
  readonly projectId: string;
  readonly appId: string;
  readonly authDomain: string;
}

export interface Env {
  /** URL base da API, sem barra no fim. */
  readonly apiUrl: string;
  readonly firebase: FirebaseConfig;
}

export function parseEnv(source: Readonly<Record<string, string | undefined>>): Env {
  const parsed = EnvSchema.safeParse(source);
  if (!parsed.success) {
    throw new Error(
      `Variáveis de ambiente inválidas: ${parsed.error.issues.map((i) => i.path.join(".")).join(", ")}`,
    );
  }
  const projectId = parsed.data.EXPO_PUBLIC_FIREBASE_PROJECT_ID;
  return {
    apiUrl: parsed.data.EXPO_PUBLIC_API_URL.replace(/\/+$/, ""),
    firebase: {
      apiKey: parsed.data.EXPO_PUBLIC_FIREBASE_API_KEY,
      projectId,
      appId: parsed.data.EXPO_PUBLIC_FIREBASE_APP_ID,
      authDomain: `${projectId}.firebaseapp.com`,
    },
  };
}

export function loadEnv(): Env {
  // Acesso direto a process.env.EXPO_PUBLIC_*: é assim que o Expo injeta o valor no bundle.
  return parseEnv({
    EXPO_PUBLIC_API_URL: process.env.EXPO_PUBLIC_API_URL,
    EXPO_PUBLIC_FIREBASE_API_KEY: process.env.EXPO_PUBLIC_FIREBASE_API_KEY,
    EXPO_PUBLIC_FIREBASE_PROJECT_ID: process.env.EXPO_PUBLIC_FIREBASE_PROJECT_ID,
    EXPO_PUBLIC_FIREBASE_APP_ID: process.env.EXPO_PUBLIC_FIREBASE_APP_ID,
  });
}
