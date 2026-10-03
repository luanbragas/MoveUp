import type { PlannedChanges, PlannedSnapshot } from "./planned";

/** GET /v1/sync. */
export interface SyncApi {
  changesSince(cursor: string | null): Promise<PlannedChanges>;
}

/**
 * O planejado no aparelho (SQLite é a fonte da verdade local). Aplicar é idempotente: o mesmo
 * lote duas vezes dá o mesmo resultado (a janela de sobreposição reenvia mudanças).
 */
export interface PlannedStore {
  cursor(): Promise<string | null>;
  /** Upsert dos programas e dos treinos (substituem os do programa); tombstone apaga. */
  apply(changes: PlannedChanges): Promise<void>;
  snapshot(): Promise<PlannedSnapshot>;
  /** Ao sair da conta: nada de outro usuário fica no aparelho. */
  clear(): Promise<void>;
}
