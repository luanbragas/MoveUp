import { createContext, useContext, type ReactNode } from "react";
import { Platform } from "react-native";
import { createAnamnesisApiRepository } from "../features/anamnesis/data/api/anamnesis-api";
import {
  createMemoryAnamnesisDraft,
  createSqliteAnamnesisDraft,
} from "../features/anamnesis/data/draft-store";
import type { AnamnesisDraftStore, AnamnesisRepository } from "../features/anamnesis/domain/ports";
import { createAlertsApiRepository } from "../features/alerts/data/api/alerts-api";
import {
  createExpoPushTokenSource,
  createNoPushTokenSource,
} from "../features/alerts/data/push-token";
import type { AlertsRepository, PushTokenSource } from "../features/alerts/domain/ports";
import { createAccountApiRepository } from "../features/auth/data/api/account-api";
import { createMeApiRepository } from "../features/auth/data/api/me-api";
import type { AccountRepository, MeRepository } from "../features/auth/domain/ports";
import type { AuthSession } from "../features/auth/domain/session";
import { createClientsApiRepository } from "../features/clients/data/api/clients-api";
import type { ClientsRepository } from "../features/clients/domain/ports";
import { createExercisesApiRepository } from "../features/exercise-library/data/api/exercises-api";
import type { ExercisesRepository } from "../features/exercise-library/domain/ports";
import {
  createMemorySessionStore,
  createSqliteSessionStore,
} from "../features/execution/data/session-stores";
import { createNoRestAlarm, createRestAlarm } from "../features/execution/data/rest-alarm";
import { createSessionSyncApi } from "../features/execution/data/session-sync-api";
import type { RestAlarm, SessionStore, SessionSyncApi } from "../features/execution/domain/ports";
import { createSyncApi } from "../features/sync/data/api/sync-api";
import { createMemoryStore } from "../features/sync/data/memory-store";
import { createSqliteStore } from "../features/sync/data/sqlite/sqlite-store";
import type { PlannedStore, SyncApi } from "../features/sync/domain/ports";
import { createTrainingApiRepository } from "../features/training/data/api/training-api";
import {
  createMemoryDraftStore,
  createSqliteDraftStore,
} from "../features/training/data/draft-store";
import type { TrainingRepository, WorkoutDraftStore } from "../features/training/domain/ports";
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
  readonly syncApi: SyncApi;
  readonly plannedStore: PlannedStore;
  readonly sessionStore: SessionStore;
  readonly sessionSyncApi: SessionSyncApi;
  readonly restAlarm: RestAlarm;
  readonly workoutDrafts: WorkoutDraftStore;
  readonly alerts: AlertsRepository;
  readonly pushTokens: PushTokenSource;
  readonly anamnesis: AnamnesisRepository;
  readonly anamnesisDraft: AnamnesisDraftStore;
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
    syncApi: createSyncApi(),
    // web (prévia) não tem SQLite nativo: memória
    plannedStore: Platform.OS === "web" ? createMemoryStore() : createSqliteStore(),
    sessionStore: Platform.OS === "web" ? createMemorySessionStore() : createSqliteSessionStore(),
    sessionSyncApi: createSessionSyncApi(),
    restAlarm: Platform.OS === "web" ? createNoRestAlarm() : createRestAlarm(),
    workoutDrafts: Platform.OS === "web" ? createMemoryDraftStore() : createSqliteDraftStore(),
    alerts: createAlertsApiRepository(),
    pushTokens: Platform.OS === "web" ? createNoPushTokenSource() : createExpoPushTokenSource(),
    anamnesis: createAnamnesisApiRepository(),
    anamnesisDraft:
      Platform.OS === "web" ? createMemoryAnamnesisDraft() : createSqliteAnamnesisDraft(),
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
