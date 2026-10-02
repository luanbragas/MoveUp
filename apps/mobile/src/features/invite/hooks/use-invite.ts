import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useRepositories } from "../../../providers/repositories";

/** Vínculo muda pouco do lado do aluno: 1 minuto de cache. */
const LINKS_STALE_TIME_MS = 60 * 1000;

export const inviteKeys = {
  all: ["invite"] as const,
  myLinks: () => [...inviteKeys.all, "my-links"] as const,
  preview: (code: string) => [...inviteKeys.all, "preview", code] as const,
};

export function useMyLinks() {
  const { invite } = useRepositories();
  return useQuery({
    queryKey: inviteKeys.myLinks(),
    queryFn: () => invite.myLinks(),
    staleTime: LINKS_STALE_TIME_MS,
  });
}

/** Prévia do convite; sem código válido, não busca. */
export function useInvitePreview(code: string | null) {
  const { invite } = useRepositories();
  return useQuery({
    queryKey: inviteKeys.preview(code ?? ""),
    queryFn: () => invite.preview(code ?? ""),
    enabled: code !== null,
    staleTime: 0,
  });
}

export function useAcceptInvite() {
  const { invite } = useRepositories();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (code: string) => invite.accept(code),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: inviteKeys.all }),
  });
}

export function useEndMyLink() {
  const { invite } = useRepositories();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (linkId: string) => invite.endMyLink(linkId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: inviteKeys.myLinks() }),
  });
}
