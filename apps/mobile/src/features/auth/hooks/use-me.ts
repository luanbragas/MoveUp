import { useQuery } from "@tanstack/react-query";
import { useRepositories } from "../../../providers/repositories";
import { authKeys } from "./auth-keys";

/** A conta muda pouco: 5 minutos de cache antes de buscar de novo. */
const ME_STALE_TIME_MS = 5 * 60 * 1000;
/** Esperando o responsável autorizar (fora do app): confere a cada 10 s com a tela aberta. */
const GUARDIAN_POLL_MS = 10 * 1000;

/** Conta do usuário logado. Sem uid (sem sessão), não busca. */
export function useMe(uid: string | null) {
  const { me } = useRepositories();
  return useQuery({
    queryKey: authKeys.me(uid ?? "anonymous"),
    queryFn: () => me.getMe(),
    staleTime: ME_STALE_TIME_MS,
    enabled: uid !== null,
    refetchInterval: (query) =>
      query.state.data?.onboarding.guardianRequest?.status === "pending" ? GUARDIAN_POLL_MS : false,
  });
}
