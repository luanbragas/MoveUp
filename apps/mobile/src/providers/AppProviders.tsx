import { configureApiClient } from "@moveup/api-client";
import { QueryClientProvider } from "@tanstack/react-query";
import { useState, type ReactNode } from "react";
import { loadEnv } from "../shared/lib/env";
import { connectQueryToDevice, createQueryClient } from "./query-client";
import { RepositoriesProvider, createRepositories } from "./repositories";

let apiConfigured = false;

function configureApiOnce(): void {
  if (apiConfigured) {
    return;
  }
  apiConfigured = true;
  const env = loadEnv(); // lança se o env for inválido: o app não sobe
  configureApiClient({
    baseUrl: env.apiUrl,
    // Fase 1: ID token do Firebase Auth (guardado só no expo-secure-store).
    getToken: () => Promise.resolve(null),
  });
}

/** Composition root do app: cliente da API, TanStack Query e repositórios. */
export function AppProviders({ children }: { readonly children: ReactNode }) {
  const [queryClient] = useState(() => {
    configureApiOnce();
    connectQueryToDevice();
    return createQueryClient();
  });
  const [repositories] = useState(createRepositories);

  return (
    <QueryClientProvider client={queryClient}>
      <RepositoriesProvider repositories={repositories}>{children}</RepositoriesProvider>
    </QueryClientProvider>
  );
}
