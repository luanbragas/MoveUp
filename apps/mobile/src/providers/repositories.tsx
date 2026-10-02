import { createContext, useContext, type ReactNode } from "react";
import { createAccountApiRepository } from "../features/auth/data/api/account-api";
import { createMeApiRepository } from "../features/auth/data/api/me-api";
import type { AccountRepository, MeRepository } from "../features/auth/domain/ports";
import type { AuthSession } from "../features/auth/domain/session";

/** Todas as portas que os hooks usam. Nos testes, o provider recebe fakes. */
export interface Repositories {
  readonly session: AuthSession;
  readonly me: MeRepository;
  readonly account: AccountRepository;
}

/** Composition root: adaptadores reais (a sessão vem pronta: depende do env). */
export function createRepositories(session: AuthSession): Repositories {
  return { session, me: createMeApiRepository(), account: createAccountApiRepository() };
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
