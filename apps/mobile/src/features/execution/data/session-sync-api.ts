import { pushSessions, schemas, type SessionSync } from "@moveup/api-client";
import { callApi } from "../../../shared/lib/http";
import type { SessionSyncApi } from "../domain/ports";

/** Sem valor = ausente (a API espera ausência, não null). */
function compact(value: unknown): unknown {
  if (Array.isArray(value)) {
    return value.map(compact);
  }
  if (value !== null && typeof value === "object") {
    return Object.fromEntries(
      Object.entries(value)
        .filter(([, v]) => v !== null && v !== undefined)
        .map(([k, v]) => [k, compact(v)]),
    );
  }
  return value;
}

export function createSessionSyncApi(): SessionSyncApi {
  return {
    async push(sessions) {
      const body = compact({ sessions }) as SessionSync;
      return callApi(() => pushSessions(body), schemas.PushSessions200Response);
    },
  };
}
