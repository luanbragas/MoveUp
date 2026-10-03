import { useMutation } from "@tanstack/react-query";
import { useRepositories } from "../../../providers/repositories";
import { useStartSession } from "../../execution";
import type { Workout } from "../domain/workout";
import type { PlannedInput } from "../../execution";

/**
 * Treino presencial: busca a versão atual do treino e inicia a sessão no aparelho do personal
 * (performed_by = professional). Depois, a mesma execução e o mesmo envio do aluno.
 */
export function useStartPresencial(toPlanned: (workout: Workout) => PlannedInput) {
  const { training } = useRepositories();
  const start = useStartSession();
  return useMutation({
    mutationFn: async (workoutId: string) => {
      const workout = await training.getWorkout(workoutId);
      return start.mutateAsync({ planned: toPlanned(workout), performedBy: "professional" });
    },
  });
}
