import { toAppError } from "../../../shared/lib/http";
import { entryDestination, type AccountLookup, type Destination } from "../domain/entry";
import type { Me } from "../domain/me";
import { useAuthState } from "./use-auth-state";
import { useMe } from "./use-me";

/** Destino ao abrir o app, a conta (se já carregou) e a ação de buscar de novo. */
export function useEntry(): {
  readonly destination: Destination;
  readonly me: Me | null;
  readonly retry: () => void;
} {
  const auth = useAuthState();
  const uid = auth.status === "signed-in" ? auth.user.uid : null;
  const me = useMe(uid);

  let account: AccountLookup;
  if (me.data !== undefined) {
    account = { kind: "found", me: me.data };
  } else if (me.isError) {
    const error = toAppError(me.error);
    account =
      error.kind === "problem" && error.code === "account-not-registered"
        ? { kind: "not-registered" }
        : { kind: "failed" };
  } else {
    account = { kind: "loading" };
  }

  const signedIn = auth.status === "loading" ? null : auth.status === "signed-in";
  return {
    destination: entryDestination(signedIn, account),
    me: me.data ?? null,
    retry: () => {
      void me.refetch();
    },
  };
}
