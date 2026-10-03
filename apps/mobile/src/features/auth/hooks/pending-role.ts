import { useSyncExternalStore } from "react";
import type { AccountRole } from "../domain/me";

// Papel escolhido nas boas-vindas ("Sou personal" / "Tenho um convite") ou pelo link do convite,
// antes do login: o cadastro já começa nele. Só em memória; sem escolha, o cadastro pergunta.

let pendingRole: AccountRole | null = null;
const listeners = new Set<() => void>();

export function setPendingRole(role: AccountRole | null): void {
  pendingRole = role;
  listeners.forEach((listener) => {
    listener();
  });
}

export function usePendingRole(): AccountRole | null {
  return useSyncExternalStore(
    (listener) => {
      listeners.add(listener);
      return () => listeners.delete(listener);
    },
    () => pendingRole,
  );
}
