import { configureApiClient } from "@moveup/api-client";
import { QueryClientProvider } from "@tanstack/react-query";
import { useState, type ReactNode } from "react";
import { createFirebaseSession } from "../features/auth/data/firebase/firebase-session";
import type { AuthSession } from "../features/auth/domain/session";
import { loadEnv } from "../shared/lib/env";
import { connectQueryToDevice, createQueryClient } from "./query-client";
import { RepositoriesProvider, createRepositories } from "./repositories";

let session: AuthSession | null = null;

/** Lê o env (lança se inválido: o app não sobe), cria a sessão e liga o api-client a ela. */
function bootstrapOnce(): AuthSession {
  if (session !== null) {
    return session;
  }
  const env = loadEnv();
  const created = createFirebaseSession(env.firebase);
  configureApiClient({ baseUrl: env.apiUrl, getToken: () => created.idToken() });
  connectQueryToDevice();
  session = created;
  return created;
}

/** Composition root do app: sessão, cliente da API, TanStack Query e repositórios. */
export function AppProviders({ children }: { readonly children: ReactNode }) {
  const [repositories] = useState(() => createRepositories(bootstrapOnce()));
  const [queryClient] = useState(createQueryClient);

  return (
    <QueryClientProvider client={queryClient}>
      <RepositoriesProvider repositories={repositories}>{children}</RepositoriesProvider>
    </QueryClientProvider>
  );
}
