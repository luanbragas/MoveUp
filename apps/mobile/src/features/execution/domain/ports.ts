import type { Session, toSyncPayload } from "./session";

/** Sessões no aparelho (SQLite): a fonte da verdade enquanto não sincroniza. */
export interface SessionStore {
  save(session: Session): Promise<void>;
  get(sessionId: string): Promise<Session | null>;
  /** Sessão em andamento (o app foi fechado no meio do treino). */
  active(): Promise<Session | null>;
  /** Finalizadas, da mais recente para a mais antiga. */
  history(): Promise<readonly Session[]>;
  /** Última finalizada do mesmo treino, antes desta. */
  previousOf(session: Session): Promise<Session | null>;
  pending(): Promise<readonly Session[]>;
  /** Marca como enviada só se não foi editada depois do envio. */
  markSynced(sessions: readonly Pick<Session, "id" | "clientUpdatedAt">[]): Promise<void>;
  clear(): Promise<void>;
}

/** POST /v1/sync. */
export interface SessionSyncApi {
  push(sessions: readonly ReturnType<typeof toSyncPayload>[]): Promise<{
    readonly written: readonly string[];
    readonly unchanged: readonly string[];
  }>;
}

/**
 * Aviso de fim do descanso que toca com a tela bloqueada ou o app em segundo plano
 * (notificação local agendada; nada sai do aparelho). Sem permissão, não agenda e não quebra.
 */
export interface RestAlarm {
  /** Agenda para o horário (ms); devolve o id para cancelar, ou null se não deu. */
  schedule(endsAt: number): Promise<string | null>;
  cancel(id: string): Promise<void>;
}
