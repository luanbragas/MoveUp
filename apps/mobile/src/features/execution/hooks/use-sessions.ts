import { onlineManager, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect } from "react";
import { AppState } from "react-native";
import { useRepositories } from "../../../providers/repositories";
import { toSyncPayload, type Session } from "../domain/session";

export const sessionKeys = {
  all: ["sessions"] as const,
  active: () => [...sessionKeys.all, "active"] as const,
  one: (id: string) => [...sessionKeys.all, "one", id] as const,
  history: () => [...sessionKeys.all, "history"] as const,
  previous: (id: string) => [...sessionKeys.all, "previous", id] as const,
};

/** Tudo daqui lê do aparelho: funciona sem internet. */
const LOCAL = { networkMode: "always", staleTime: Number.POSITIVE_INFINITY } as const;

export function useActiveSession() {
  const { sessionStore } = useRepositories();
  return useQuery({
    queryKey: sessionKeys.active(),
    queryFn: () => sessionStore.active(),
    ...LOCAL,
  });
}

export function useSession(id: string) {
  const { sessionStore } = useRepositories();
  return useQuery({ queryKey: sessionKeys.one(id), queryFn: () => sessionStore.get(id), ...LOCAL });
}

export function useHistory() {
  const { sessionStore } = useRepositories();
  return useQuery({
    queryKey: sessionKeys.history(),
    queryFn: () => sessionStore.history(),
    ...LOCAL,
  });
}

export function usePreviousSession(session: Session | null) {
  const { sessionStore } = useRepositories();
  return useQuery({
    queryKey: sessionKeys.previous(session?.id ?? "none"),
    queryFn: () => (session === null ? null : sessionStore.previousOf(session)),
    enabled: session !== null,
    ...LOCAL,
  });
}

/** Envia as sessões pendentes; as aceitas (ou já iguais no servidor) viram enviadas. */
export function usePushSessions() {
  const { sessionStore, sessionSyncApi } = useRepositories();
  const queryClient = useQueryClient();
  return useMutation({
    mutationKey: [...sessionKeys.all, "push"],
    mutationFn: async () => {
      const pending = await sessionStore.pending();
      if (pending.length === 0) {
        return 0;
      }
      const result = await sessionSyncApi.push(pending.map(toSyncPayload));
      const accepted = new Set([...result.written, ...result.unchanged]);
      await sessionStore.markSynced(pending.filter((s) => accepted.has(s.id)));
      return accepted.size;
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: sessionKeys.all }),
  });
}

/**
 * Grava cada ação do treino no aparelho na hora (fechar o app no meio não perde nada). Ao
 * finalizar, tenta enviar; sem internet, fica pendente e vai depois.
 */
export function useSaveSession() {
  const { sessionStore } = useRepositories();
  const queryClient = useQueryClient();
  const push = usePushSessions();
  return useMutation({
    networkMode: "always",
    mutationFn: async (session: Session) => {
      await sessionStore.save(session);
      return session;
    },
    onSuccess: (session) => {
      queryClient.setQueryData(sessionKeys.one(session.id), session);
      queryClient.setQueryData(
        sessionKeys.active(),
        session.status === "in_progress" ? session : null,
      );
      if (session.status !== "in_progress") {
        void queryClient.invalidateQueries({ queryKey: sessionKeys.history() });
        push.mutate();
      }
    },
  });
}

/** Envia ao abrir, ao voltar para o app e quando a internet volta. */
export function useAutoPush() {
  const { mutate } = usePushSessions();
  useEffect(() => {
    mutate();
    const app = AppState.addEventListener("change", (state) => {
      if (state === "active") {
        mutate();
      }
    });
    const online = onlineManager.subscribe((isOnline) => {
      if (isOnline) {
        mutate();
      }
    });
    return () => {
      app.remove();
      online();
    };
  }, [mutate]);
}
