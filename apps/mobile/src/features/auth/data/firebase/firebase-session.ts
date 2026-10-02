import { FirebaseError, getApps, initializeApp } from "firebase/app";
import {
  createUserWithEmailAndPassword,
  getReactNativePersistence,
  initializeAuth,
  onAuthStateChanged,
  sendPasswordResetEmail,
  signInWithEmailAndPassword,
  signOut,
  type Auth,
  type User,
} from "firebase/auth";
import type { FirebaseConfig } from "../../../../shared/lib/env";
import {
  AuthFailure,
  type AuthErrorCode,
  type AuthSession,
  type AuthUser,
} from "../../domain/session";
import { createChunkedStorage } from "./secure-store-storage";

// Adaptador da porta AuthSession sobre o Firebase Auth (SDK JS, roda no Expo Go).
// Google e Apple entram com o development build (precisam de módulo nativo).

const ERROR_CODES: Readonly<Record<string, AuthErrorCode>> = {
  "auth/invalid-credential": "invalid-credentials",
  "auth/wrong-password": "invalid-credentials",
  "auth/user-not-found": "invalid-credentials",
  "auth/user-disabled": "invalid-credentials",
  "auth/email-already-in-use": "email-in-use",
  "auth/weak-password": "weak-password",
  "auth/password-does-not-meet-requirements": "weak-password",
  "auth/invalid-email": "invalid-email",
  "auth/missing-email": "invalid-email",
  "auth/too-many-requests": "too-many-requests",
  "auth/network-request-failed": "network",
};

export function toAuthFailure(error: unknown): AuthFailure {
  if (error instanceof FirebaseError) {
    return new AuthFailure(ERROR_CODES[error.code] ?? "unknown");
  }
  return new AuthFailure("unknown");
}

function toAuthUser(user: User | null): AuthUser | null {
  return user === null ? null : { uid: user.uid, email: user.email };
}

async function guarded<T>(action: () => Promise<T>): Promise<T> {
  try {
    return await action();
  } catch (error) {
    throw toAuthFailure(error);
  }
}

function createAuth(config: FirebaseConfig): Auth {
  const app =
    getApps()[0] ??
    initializeApp({
      apiKey: config.apiKey,
      authDomain: config.authDomain,
      projectId: config.projectId,
      appId: config.appId,
    });
  return initializeAuth(app, { persistence: getReactNativePersistence(createChunkedStorage()) });
}

export function createFirebaseSession(config: FirebaseConfig): AuthSession {
  const auth = createAuth(config);
  auth.languageCode = "pt-BR"; // e-mails do Firebase (redefinir senha) em português

  return {
    current: () => toAuthUser(auth.currentUser),
    onChange: (listener) =>
      onAuthStateChanged(auth, (user) => {
        listener(toAuthUser(user));
      }),
    ready: () => auth.authStateReady(),
    signIn: (email, password) =>
      guarded(async () => {
        await signInWithEmailAndPassword(auth, email.trim(), password);
      }),
    signUp: (email, password) =>
      guarded(async () => {
        await createUserWithEmailAndPassword(auth, email.trim(), password);
      }),
    sendPasswordReset: (email) => guarded(() => sendPasswordResetEmail(auth, email.trim())),
    signOut: () => guarded(() => signOut(auth)),
    idToken: async () => {
      const user = auth.currentUser;
      return user === null ? null : user.getIdToken();
    },
  };
}
