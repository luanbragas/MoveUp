import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useRepositories } from "../../../providers/repositories";

export interface Credentials {
  readonly email: string;
  readonly password: string;
}

export function useSignIn() {
  const { session } = useRepositories();
  return useMutation({
    mutationFn: ({ email, password }: Credentials) => session.signIn(email, password),
  });
}

export function useSignUp() {
  const { session } = useRepositories();
  return useMutation({
    mutationFn: ({ email, password }: Credentials) => session.signUp(email, password),
  });
}

export function useSendPasswordReset() {
  const { session } = useRepositories();
  return useMutation({ mutationFn: (email: string) => session.sendPasswordReset(email) });
}

/** Sai da conta e limpa todo o cache (nenhum dado do usuário fica na memória). */
export function useSignOut() {
  const { session, plannedStore } = useRepositories();
  const queryClient = useQueryClient();
  return useMutation({
    // o treino guardado no aparelho é do aluno que saiu: apaga antes de outra conta entrar
    mutationFn: async () => {
      await session.signOut();
      await plannedStore.clear();
    },
    onSuccess: () => {
      queryClient.clear();
    },
  });
}
