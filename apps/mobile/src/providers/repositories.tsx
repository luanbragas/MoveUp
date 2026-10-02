import { createContext, useContext, type ReactNode } from "react";
import { createMeApiRepository } from "../features/auth/data/api/me-api";
import type { MeRepository } from "../features/auth/domain/ports";

/** Todas as portas que os hooks usam. Nos testes, o provider recebe fakes. */
export interface Repositories {
  readonly me: MeRepository;
}

/** Composition root: adaptadores reais. */
export function createRepositories(): Repositories {
  return { me: createMeApiRepository() };
}

const RepositoriesContext = createContext<Repositories | null>(null);

export function RepositoriesProvider({
  repositories,
  children,
}: {
  readonly repositories: Repositories;
  readonly children: ReactNode;
}) {
  return (
    <RepositoriesContext.Provider value={repositories}>{children}</RepositoriesContext.Provider>
  );
}

export function useRepositories(): Repositories {
  const repositories = useContext(RepositoriesContext);
  if (repositories === null) {
    throw new Error("useRepositories fora do RepositoriesProvider");
  }
  return repositories;
}
