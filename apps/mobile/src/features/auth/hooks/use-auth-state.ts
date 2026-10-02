import { useEffect, useState } from "react";
import { useRepositories } from "../../../providers/repositories";
import type { AuthUser } from "../domain/session";

export type AuthState =
  | { readonly status: "loading" }
  | { readonly status: "signed-out" }
  | { readonly status: "signed-in"; readonly user: AuthUser };

function toState(user: AuthUser | null): AuthState {
  return user === null ? { status: "signed-out" } : { status: "signed-in", user };
}

/** Sessão no provedor: espera a leitura da sessão guardada e acompanha login/logout. */
export function useAuthState(): AuthState {
  const { session } = useRepositories();
  const [state, setState] = useState<AuthState>({ status: "loading" });

  useEffect(() => {
    let active = true;
    let unsubscribe: (() => void) | null = null;
    void session.ready().then(() => {
      if (!active) {
        return;
      }
      setState(toState(session.current()));
      unsubscribe = session.onChange((user) => {
        setState(toState(user));
      });
    });
    return () => {
      active = false;
      unsubscribe?.();
    };
  }, [session]);

  return state;
}
