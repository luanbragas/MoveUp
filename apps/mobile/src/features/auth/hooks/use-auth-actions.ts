import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useRepositories } from "../../../providers/repositories";
import { toSyncPayload } from "../../execution";

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
/** Há treino registrado que ainda não foi para o servidor (sem internet). */
export class PendingSessionsError extends Error {
  constructor() {
    super("pending-sessions");
  }
}

export function useSignOut() {
  const { session, plannedStore, sessionStore, sessionSyncApi, workoutDrafts, alerts, pushTokens } =
    useRepositories();
  const queryClient = useQueryClient();
  return useMutation({
    // o que está no aparelho é de quem saiu: envia o treino pendente e apaga tudo antes de outra
    // conta entrar. Sem conseguir enviar, não sai (o treino registrado não pode se perder).
    mutationFn: async () => {
      const pending = await sessionStore.pending();
      if (pending.length > 0) {
        try {
          await sessionSyncApi.push(pending.map(toSyncPayload));
        } catch {
          throw new PendingSessionsError();
        }
      }
      // o aparelho para de receber push desta conta (melhor esforço: sem rede, o token expira)
      try {
        const device = await pushTokens.current(false);
        if (device !== null) {
          await alerts.removeDevice(device.token);
        }
      } catch {
        // segue saindo
      }
      await session.signOut();
      await plannedStore.clear();
      await sessionStore.clear();
      await workoutDrafts.clear();
    },
    onSuccess: () => {
      queryClient.clear();
    },
  });
}
