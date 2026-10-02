import { useInfiniteQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useRepositories } from "../../../providers/repositories";
import type { LinkId, NewClientInput } from "../domain/client";

/** Lista muda quando o aluno aceita (em outro aparelho): 30 s de cache. */
const CLIENTS_STALE_TIME_MS = 30 * 1000;

export const clientsKeys = {
  all: ["clients"] as const,
  list: () => [...clientsKeys.all, "list"] as const,
};

/** Alunos do profissional, paginados por cursor. */
export function useClients() {
  const { clients } = useRepositories();
  return useInfiniteQuery({
    queryKey: clientsKeys.list(),
    queryFn: ({ pageParam }) => clients.list(pageParam),
    initialPageParam: null as string | null,
    getNextPageParam: (page) => page.nextCursor,
    staleTime: CLIENTS_STALE_TIME_MS,
  });
}

export function useInviteClient() {
  const { clients } = useRepositories();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: NewClientInput) => clients.invite(input),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: clientsKeys.list() }),
  });
}

export type LinkCommand = "resend" | "cancel-invite" | "inactivate" | "reactivate" | "end";

/** Ações sobre um vínculo; "resend" devolve o convite novo para compartilhar. */
export function useLinkCommand() {
  const { clients } = useRepositories();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ command, linkId }: { command: LinkCommand; linkId: LinkId }) => {
      switch (command) {
        case "resend":
          return clients.resendInvite(linkId);
        case "cancel-invite":
          await clients.cancelInvite(linkId);
          return null;
        case "inactivate":
          await clients.inactivate(linkId);
          return null;
        case "reactivate":
          await clients.reactivate(linkId);
          return null;
        case "end":
          await clients.end(linkId);
          return null;
      }
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: clientsKeys.list() }),
  });
}
