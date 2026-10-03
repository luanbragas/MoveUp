import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useRepositories } from "../../../providers/repositories";
import type { Program, ProgramInput } from "../domain/program";
import type { WorkoutDraft } from "../domain/workout";

export const trainingKeys = {
  all: ["training"] as const,
  templates: () => [...trainingKeys.all, "templates"] as const,
  workout: (id: string) => [...trainingKeys.all, "workout", id] as const,
  activeProgram: (linkId: string) => [...trainingKeys.all, "program", "active", linkId] as const,
};

export function useTemplates() {
  const { training } = useRepositories();
  return useQuery({ queryKey: trainingKeys.templates(), queryFn: () => training.listTemplates() });
}

export function useCreateTemplate() {
  const { training } = useRepositories();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (draft: WorkoutDraft) => training.createTemplate(draft),
    onSuccess: (workout) => {
      queryClient.setQueryData(trainingKeys.workout(workout.id), workout);
      return queryClient.invalidateQueries({ queryKey: trainingKeys.templates() });
    },
  });
}

/** O editor carrega uma vez e trabalha no rascunho local até salvar. */
export function useWorkout(workoutId: string) {
  const { training } = useRepositories();
  return useQuery({
    queryKey: trainingKeys.workout(workoutId),
    queryFn: () => training.getWorkout(workoutId),
    staleTime: Number.POSITIVE_INFINITY,
  });
}

export function useSaveWorkout(workoutId: string) {
  const { training } = useRepositories();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ revision, draft }: { revision: number; draft: WorkoutDraft }) =>
      training.saveWorkout(workoutId, revision, draft),
    onSuccess: (workout) => {
      queryClient.setQueryData(trainingKeys.workout(workoutId), workout);
      // listas (modelos e programa) mostram nome e quantidade de exercícios
      return queryClient.invalidateQueries({
        predicate: (query) => query.queryKey[0] === "training" && query.queryKey[1] !== "workout",
      });
    },
  });
}

export function useActiveProgram(linkId: string) {
  const { training } = useRepositories();
  return useQuery({
    queryKey: trainingKeys.activeProgram(linkId),
    queryFn: () => training.activeProgram(linkId),
  });
}

export function useCreateProgram(linkId: string) {
  const { training } = useRepositories();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: ProgramInput) => training.createProgram(linkId, input),
    onSuccess: (program) => {
      queryClient.setQueryData(trainingKeys.activeProgram(linkId), program);
    },
  });
}

export function useUpdateProgram(linkId: string) {
  const { training } = useRepositories();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      program,
      input,
      schedule,
    }: {
      program: Program;
      input: ProgramInput;
      schedule: readonly { workoutId: string; weekdays: readonly number[] }[];
    }) => training.updateProgram(program, input, schedule),
    onSuccess: (program) => {
      queryClient.setQueryData(trainingKeys.activeProgram(linkId), program);
    },
  });
}

export function useAddProgramWorkout(linkId: string) {
  const { training } = useRepositories();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      programId,
      templateId,
      name,
    }: {
      programId: string;
      templateId: string | null;
      name: string | null;
    }) => training.addProgramWorkout(programId, templateId, name),
    onSuccess: (workout) => {
      queryClient.setQueryData(trainingKeys.workout(workout.id), workout);
      return queryClient.invalidateQueries({ queryKey: trainingKeys.activeProgram(linkId) });
    },
  });
}
