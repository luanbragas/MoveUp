import { AuthFailure, type AuthSession, type AuthUser } from "../../domain/session";

/** Sessão em memória para testes: e-mail/senha cadastrados num mapa. */
export function createFakeSession(initial: AuthUser | null = null): AuthSession & {
  readonly accounts: Map<string, string>;
} {
  let current = initial;
  const accounts = new Map<string, string>();
  const listeners = new Set<(user: AuthUser | null) => void>();
  const emit = () => {
    listeners.forEach((listener) => {
      listener(current);
    });
  };

  return {
    accounts,
    current: () => current,
    onChange: (listener) => {
      listeners.add(listener);
      return () => listeners.delete(listener);
    },
    ready: () => Promise.resolve(),
    signIn: (email, password) => {
      if (accounts.get(email) !== password) {
        return Promise.reject(new AuthFailure("invalid-credentials"));
      }
      current = { uid: `uid-${email}`, email };
      emit();
      return Promise.resolve();
    },
    signUp: (email, password) => {
      if (accounts.has(email)) {
        return Promise.reject(new AuthFailure("email-in-use"));
      }
      accounts.set(email, password);
      current = { uid: `uid-${email}`, email };
      emit();
      return Promise.resolve();
    },
    sendPasswordReset: () => Promise.resolve(),
    signOut: () => {
      current = null;
      emit();
      return Promise.resolve();
    },
    idToken: () => Promise.resolve(current === null ? null : `token-${current.uid}`),
  };
}
