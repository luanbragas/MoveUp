import { onlineManager } from "@tanstack/react-query";
import { useSyncExternalStore } from "react";

/** Conexão atual, pelo onlineManager do TanStack (ligado ao NetInfo em providers/query-client). */
export function useOnline(): boolean {
  return useSyncExternalStore(
    (listener) => onlineManager.subscribe(listener),
    () => onlineManager.isOnline(),
  );
}
