// Sessão no provedor de login (Firebase), vista pelo domínio do app. A UI e os hooks
// não conhecem o Firebase: só esta porta (FRONTEND-PATTERN, seção 6).

export interface AuthUser {
  readonly uid: string;
  readonly email: string | null;
}

/** Erros de login que a UI sabe explicar (mensagens em ui/strings.ts). */
export type AuthErrorCode =
  | "invalid-credentials"
  | "email-in-use"
  | "weak-password"
  | "invalid-email"
  | "too-many-requests"
  | "network"
  | "unknown";

export class AuthFailure extends Error {
  readonly code: AuthErrorCode;

  constructor(code: AuthErrorCode) {
    super(`auth_${code}`);
    this.name = "AuthFailure";
    this.code = code;
  }
}

export interface AuthSession {
  /** Usuário atual (null sem sessão). Pode mudar: assine com onChange. */
  current(): AuthUser | null;
  /** Avisa a cada login/logout; devolve a função que cancela a assinatura. */
  onChange(listener: (user: AuthUser | null) => void): () => void;
  /** Resolve quando a sessão guardada no aparelho já foi lida. */
  ready(): Promise<void>;
  signIn(email: string, password: string): Promise<void>;
  signUp(email: string, password: string): Promise<void>;
  sendPasswordReset(email: string): Promise<void>;
  signOut(): Promise<void>;
  /** ID token atual (renovado pelo provedor quando expira), ou null sem sessão. */
  idToken(): Promise<string | null>;
}
