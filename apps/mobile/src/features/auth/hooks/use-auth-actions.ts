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
  const { session } = useRepositories();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => session.signOut(),
    onSuccess: () => {
      queryClient.clear();
    },
  });
}
