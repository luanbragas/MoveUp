import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect } from "react";
import { AppState } from "react-native";
import { useRepositories } from "../../../providers/repositories";

export const plannedKeys = {
  all: ["planned"] as const,
  snapshot: () => [...plannedKeys.all, "snapshot"] as const,
};

/** O que está no aparelho: funciona sem internet. */
export function usePlannedSnapshot() {
  const { plannedStore } = useRepositories();
  return useQuery({
    queryKey: plannedKeys.snapshot(),
    queryFn: () => plannedStore.snapshot(),
    staleTime: Number.POSITIVE_INFINITY,
    networkMode: "always", // lê do SQLite, não da rede
  });
}

/** Baixa o que mudou desde o cursor e grava no aparelho. */
export function useSyncPlanned() {
  const { syncApi, plannedStore } = useRepositories();
  const queryClient = useQueryClient();
  return useMutation({
    mutationKey: [...plannedKeys.all, "sync"],
    mutationFn: async () => {
      const changes = await syncApi.changesSince(await plannedStore.cursor());
      await plannedStore.apply(changes);
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: plannedKeys.snapshot() }),
  });
}

/** Sincroniza ao abrir a tela e sempre que o app volta para a frente. */
export function useAutoSync() {
  const sync = useSyncPlanned();
  const { mutate } = sync;
  useEffect(() => {
    mutate();
    const subscription = AppState.addEventListener("change", (state) => {
      if (state === "active") {
        mutate();
      }
    });
    return () => {
      subscription.remove();
    };
  }, [mutate]);
  return sync;
}
