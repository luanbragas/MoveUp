import { createContext, useContext, type ReactNode } from "react";
import { createAccountApiRepository } from "../features/auth/data/api/account-api";
import { createMeApiRepository } from "../features/auth/data/api/me-api";
import type { AccountRepository, MeRepository } from "../features/auth/domain/ports";
import type { AuthSession } from "../features/auth/domain/session";
import { createClientsApiRepository } from "../features/clients/data/api/clients-api";
import type { ClientsRepository } from "../features/clients/domain/ports";
import { createExercisesApiRepository } from "../features/exercise-library/data/api/exercises-api";
import type { ExercisesRepository } from "../features/exercise-library/domain/ports";
import { createTrainingApiRepository } from "../features/training/data/api/training-api";
import type { TrainingRepository } from "../features/training/domain/ports";
import { createInviteApiRepository } from "../features/invite/data/api/invite-api";
import type { InviteRepository } from "../features/invite/domain/ports";

/** Todas as portas que os hooks usam. Nos testes, o provider recebe fakes. */
export interface Repositories {
  readonly session: AuthSession;
  readonly me: MeRepository;
  readonly account: AccountRepository;
  readonly clients: ClientsRepository;
  readonly invite: InviteRepository;
  readonly exercises: ExercisesRepository;
  readonly training: TrainingRepository;
}

/** Composition root: adaptadores reais (a sessão vem pronta: depende do env). */
export function createRepositories(session: AuthSession): Repositories {
  return {
    session,
    me: createMeApiRepository(),
    account: createAccountApiRepository(),
    clients: createClientsApiRepository(),
    invite: createInviteApiRepository(),
    exercises: createExercisesApiRepository(),
    training: createTrainingApiRepository(),
  };
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
