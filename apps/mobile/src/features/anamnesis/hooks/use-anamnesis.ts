import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useRepositories } from "../../../providers/repositories";
import type { Answers, Clearance, RestrictionInput } from "../domain/anamnesis";

/** O modelo muda raramente: 1 hora de cache. */
const TEMPLATE_STALE_TIME_MS = 60 * 60 * 1000;

export const anamnesisKeys = {
  all: ["anamnesis"] as const,
  template: () => [...anamnesisKeys.all, "template"] as const,
  mine: () => [...anamnesisKeys.all, "mine"] as const,
  draft: () => [...anamnesisKeys.all, "draft"] as const,
  client: (linkId: string) => [...anamnesisKeys.all, "client", linkId] as const,
  restrictions: (linkId: string) => [...anamnesisKeys.all, "restrictions", linkId] as const,
};

export function useAnamnesisTemplate() {
  const { anamnesis } = useRepositories();
  return useQuery({
    queryKey: anamnesisKeys.template(),
    queryFn: () => anamnesis.template(),
    staleTime: TEMPLATE_STALE_TIME_MS,
  });
}

export function useMyAnamnesis() {
  const { anamnesis } = useRepositories();
  return useQuery({ queryKey: anamnesisKeys.mine(), queryFn: () => anamnesis.mine() });
}

export function useAnamnesisDraft() {
  const { anamnesisDraft } = useRepositories();
  return useQuery({
    queryKey: anamnesisKeys.draft(),
    queryFn: () => anamnesisDraft.get(),
    staleTime: Number.POSITIVE_INFINITY,
  });
}

/** Grava o rascunho sem esperar (cada resposta já fica no aparelho). */
export function useSaveAnamnesisDraft() {
  const { anamnesisDraft } = useRepositories();
  const queryClient = useQueryClient();
  return (answers: Answers) => {
    queryClient.setQueryData(anamnesisKeys.draft(), answers);
    void anamnesisDraft.save(answers).catch(() => undefined);
  };
}

export function useSubmitAnamnesis() {
  const { anamnesis, anamnesisDraft } = useRepositories();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (answers: Answers) => {
      const saved = await anamnesis.submit(answers);
      await anamnesisDraft.clear();
      return saved;
    },
    onSuccess: (saved) => {
      queryClient.setQueryData(anamnesisKeys.mine(), saved);
      queryClient.setQueryData(anamnesisKeys.draft(), null);
    },
  });
}

export function useClientAnamnesis(linkId: string) {
  const { anamnesis } = useRepositories();
  return useQuery({
    queryKey: anamnesisKeys.client(linkId),
    queryFn: () => anamnesis.ofClient(linkId),
  });
}

export function useReviewAnamnesis(linkId: string) {
  const { anamnesis } = useRepositories();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { answers: Answers; clearance: Clearance; clearanceDate: string | null }) =>
      anamnesis.review(linkId, input.answers, input.clearance, input.clearanceDate),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: anamnesisKeys.client(linkId) }),
  });
}

/** Restrições do aluno (ativas e resolvidas; a tela filtra). */
export function useRestrictions(linkId: string | null) {
  const { anamnesis } = useRepositories();
  return useQuery({
    queryKey: anamnesisKeys.restrictions(linkId ?? ""),
    queryFn: () => anamnesis.restrictions(linkId ?? "", true),
    enabled: linkId !== null,
  });
}

export function useSaveRestriction(linkId: string) {
  const { anamnesis } = useRepositories();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, input }: { id: string | null; input: RestrictionInput }) =>
      id === null
        ? anamnesis.createRestriction(linkId, input)
        : anamnesis.updateRestriction(linkId, id, input),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: anamnesisKeys.restrictions(linkId) }),
  });
}

export function useDeleteRestriction(linkId: string) {
  const { anamnesis } = useRepositories();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => anamnesis.deleteRestriction(linkId, id),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: anamnesisKeys.restrictions(linkId) }),
  });
}
