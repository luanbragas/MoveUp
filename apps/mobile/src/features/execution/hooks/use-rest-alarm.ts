import { useEffect } from "react";
import { useRepositories } from "../../../providers/repositories";

/**
 * Enquanto o descanso está na tela, deixa agendado o aviso do fim (toca com a tela bloqueada).
 * Mudou o horário (+30 s) ou saiu do descanso: cancela o anterior.
 */
export function useRestAlarm(endsAt: number) {
  const { restAlarm } = useRepositories();
  useEffect(() => {
    let cancelled = false;
    let id: string | null = null;
    void restAlarm.schedule(endsAt).then((scheduled) => {
      id = scheduled;
      if (cancelled && id !== null) {
        void restAlarm.cancel(id);
      }
    });
    return () => {
      cancelled = true;
      if (id !== null) {
        void restAlarm.cancel(id);
      }
    };
  }, [endsAt, restAlarm]);
}
