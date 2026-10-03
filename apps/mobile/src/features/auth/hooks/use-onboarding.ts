import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useRepositories } from "../../../providers/repositories";
import type { ConsentKind } from "../domain/me";
import type { GuardianInput, LegalVersions, RegisterInput } from "../domain/ports";
import { authKeys } from "./auth-keys";

/** Versões dos textos mudam raramente: 1 hora de cache. */
const LEGAL_STALE_TIME_MS = 60 * 60 * 1000;

export function useLegalVersions() {
  const { account } = useRepositories();
  return useQuery({
    queryKey: authKeys.legalVersions(),
    queryFn: () => account.legalVersions(),
    staleTime: LEGAL_STALE_TIME_MS,
  });
}

/** Cria a conta; a resposta já é o "me" do usuário, guardado no cache. */
export function useRegisterAccount(uid: string) {
  const { account } = useRepositories();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: RegisterInput) => account.register(input),
    onSuccess: (me) => {
      queryClient.setQueryData(authKeys.me(uid), me);
    },
  });
}

export function useGrantConsents(uid: string) {
  const { account } = useRepositories();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ kinds, versions }: { kinds: readonly ConsentKind[]; versions: LegalVersions }) =>
      account.grantConsents(kinds, versions),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: authKeys.me(uid) }),
  });
}

/** Menor indica o responsável; a resposta traz o link para compartilhar com ele. */
export function useRequestGuardian(uid: string) {
  const { account } = useRepositories();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ input, versions }: { input: GuardianInput; versions: LegalVersions }) =>
      account.requestGuardian(input, versions),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: authKeys.me(uid) }),
  });
}

/** Link novo para o mesmo pedido (o anterior deixa de valer). */
export function useResendGuardianLink(uid: string) {
  const { account } = useRepositories();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => account.resendGuardianLink(),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: authKeys.me(uid) }),
  });
}

/** Desiste do pedido para indicar outra pessoa. */
export function useCancelGuardianRequest(uid: string) {
  const { account } = useRepositories();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => account.cancelGuardianRequest(),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: authKeys.me(uid) }),
  });
}
