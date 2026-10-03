import {
  useInfiniteQuery,
  useMutation,
  useQuery,
  useQueryClient,
  type InfiniteData,
} from "@tanstack/react-query";
import { useEffect } from "react";
import { useRepositories } from "../../../providers/repositories";
import type { AlertPage, AlertSetting, AlertStatus } from "../domain/alert";

/** Alertas chegam do worker a qualquer momento: 30 s de cache e recarrega ao voltar à aba. */
const ALERTS_STALE_TIME_MS = 30 * 1000;

export const alertsKeys = {
  all: ["alerts"] as const,
  list: (status: AlertStatus) => [...alertsKeys.all, "list", status] as const,
  settings: () => [...alertsKeys.all, "settings"] as const,
};

export function useAlerts(status: AlertStatus) {
  const { alerts } = useRepositories();
  return useInfiniteQuery({
    queryKey: alertsKeys.list(status),
    queryFn: ({ pageParam }) => alerts.list(status, pageParam),
    initialPageParam: null as string | null,
    getNextPageParam: (page) => page.next,
    staleTime: ALERTS_STALE_TIME_MS,
  });
}

type Pages = InfiniteData<AlertPage, string | null>;

/** Resolver e adiar tiram o alerta da lista na hora (e desfazem se o servidor recusar). */
function useAlertAction(run: (alertId: string, days: number) => Promise<void>) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ alertId, days }: { alertId: string; days: number }) => run(alertId, days),
    onMutate: async ({ alertId }) => {
      await queryClient.cancelQueries({ queryKey: alertsKeys.list("open") });
      const before = queryClient.getQueryData<Pages>(alertsKeys.list("open"));
      queryClient.setQueryData<Pages>(alertsKeys.list("open"), (data) =>
        data === undefined
          ? data
          : {
              ...data,
              pages: data.pages.map((p) => ({
                ...p,
                items: p.items.filter((a) => a.id !== alertId),
                openCount: Math.max(0, p.openCount - 1),
              })),
            },
      );
      return { before };
    },
    onError: (_error, _vars, context) => {
      if (context?.before !== undefined) {
        queryClient.setQueryData(alertsKeys.list("open"), context.before);
      }
    },
    onSettled: () => queryClient.invalidateQueries({ queryKey: alertsKeys.all }),
  });
}

export function useResolveAlert() {
  const { alerts } = useRepositories();
  return useAlertAction((alertId) => alerts.resolve(alertId));
}

export function useSnoozeAlert() {
  const { alerts } = useRepositories();
  return useAlertAction((alertId, days) => alerts.snooze(alertId, days));
}

export function useAlertSettings() {
  const { alerts } = useRepositories();
  return useQuery({ queryKey: alertsKeys.settings(), queryFn: () => alerts.settings() });
}

export function useUpdateAlertSettings() {
  const { alerts } = useRepositories();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (changes: readonly AlertSetting[]) => alerts.updateSettings(changes),
    onSuccess: (settings) => {
      queryClient.setQueryData(alertsKeys.settings(), settings);
    },
  });
}

/**
 * Registra este aparelho para push do profissional (uma vez por abertura). Sem token (Expo Go,
 * sem permissão, sem projeto EAS) não faz nada: a central continua funcionando sem push.
 */
export function usePushRegistration() {
  const { alerts, pushTokens } = useRepositories();
  useEffect(() => {
    let cancelled = false;
    void pushTokens
      .current(true)
      .then((device) => {
        if (device !== null && !cancelled) {
          return alerts.registerDevice(device.token, device.platform);
        }
        return undefined;
      })
      .catch(() => undefined);
    return () => {
      cancelled = true;
    };
  }, [alerts, pushTokens]);
}
