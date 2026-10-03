import { useQuery } from "@tanstack/react-query";
import { useMemo } from "react";
import { useRepositories } from "../../../providers/repositories";
import type { WorkoutDraft } from "../domain/workout";
import { trainingKeys } from "./use-training";

/** Rascunho do editor guardado no celular (lido uma vez ao abrir o treino). */
export function useStoredDraft(workoutId: string) {
  const { workoutDrafts } = useRepositories();
  return useQuery({
    queryKey: [...trainingKeys.workout(workoutId), "draft"],
    queryFn: () => workoutDrafts.get(workoutId),
    staleTime: Number.POSITIVE_INFINITY,
    gcTime: 0,
  });
}

/** Grava e apaga o rascunho; erros do armazenamento local não travam o editor. */
export function useDraftWriter(workoutId: string) {
  const { workoutDrafts } = useRepositories();
  return useMemo(
    () => ({
      save(draft: WorkoutDraft, baseRevision: number) {
        void workoutDrafts
          .save(workoutId, { draft, baseRevision, savedAt: new Date().toISOString() })
          .catch(() => undefined);
      },
      remove() {
        void workoutDrafts.remove(workoutId).catch(() => undefined);
      },
    }),
    [workoutDrafts, workoutId],
  );
}
