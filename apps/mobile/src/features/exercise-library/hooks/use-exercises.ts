import {
  keepPreviousData,
  useMutation,
  useQueries,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { useRepositories } from "../../../providers/repositories";
import type { Exercise, MuscleCode, NewExerciseInput } from "../domain/exercise";

/** A biblioteca muda pouco: 10 minutos de cache por busca. */
const LIBRARY_STALE_TIME_MS = 10 * 60 * 1000;

export const exerciseKeys = {
  all: ["exercises"] as const,
  search: (query: string, muscle: MuscleCode | null) =>
    [...exerciseKeys.all, "search", query.trim().toLowerCase(), muscle ?? "all"] as const,
};

/** Busca na biblioteca; mantém o resultado anterior na tela enquanto a nova busca chega. */
export function useExerciseSearch(query: string, muscle: MuscleCode | null) {
  const { exercises } = useRepositories();
  return useQuery({
    queryKey: exerciseKeys.search(query, muscle),
    queryFn: () => exercises.search(query, muscle),
    staleTime: LIBRARY_STALE_TIME_MS,
    placeholderData: keepPreviousData,
  });
}

export function useCreateExercise() {
  const { exercises } = useRepositories();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: NewExerciseInput) => exercises.create(input),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: exerciseKeys.all }),
  });
}

export function useArchiveExercise() {
  const { exercises } = useRepositories();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (exerciseId: string) => exercises.archive(exerciseId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: exerciseKeys.all }),
  });
}

/**
 * A biblioteca dos músculos pedidos, num mapa por id (treino presencial: como fazer e troca
 * pelo mesmo músculo). Uma busca por músculo, com o mesmo cache da tela da biblioteca.
 */
export function useExercisesByMuscle(muscles: readonly MuscleCode[]) {
  const { exercises } = useRepositories();
  return useQueries({
    queries: [...new Set(muscles)].map((muscle) => ({
      queryKey: exerciseKeys.search("", muscle),
      queryFn: () => exercises.search("", muscle),
      staleTime: LIBRARY_STALE_TIME_MS,
    })),
    combine: (results) => {
      const map = new Map<string, Exercise>();
      results.forEach((r) => r.data?.forEach((e) => map.set(e.id, e)));
      return map;
    },
  });
}
