import { useSyncExternalStore } from "react";

// Código do convite que chegou pelo link (moveup.com.br/i/CODIGO) antes do login e do
// cadastro: fica guardado em memória até a casa do aluno usá-lo. Não é segredo de longo
// prazo (o convite expira e é de uso único) e some ao fechar o app.

let pendingCode: string | null = null;
const listeners = new Set<() => void>();

export function setPendingInviteCode(code: string | null): void {
  pendingCode = code;
  listeners.forEach((listener) => {
    listener();
  });
}

export function usePendingInviteCode(): string | null {
  return useSyncExternalStore(
    (listener) => {
      listeners.add(listener);
      return () => listeners.delete(listener);
    },
    () => pendingCode,
  );
}
