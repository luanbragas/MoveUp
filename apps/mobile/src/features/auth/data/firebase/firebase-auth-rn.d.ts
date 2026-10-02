// O build React Native do Firebase Auth (o que o Metro carrega pelo export condition
// "react-native") exporta getReactNativePersistence, mas os tipos padrão do pacote
// não o declaram. Assinatura conforme @firebase/auth/dist/rn/index.rn.d.ts.
import type { Persistence } from "firebase/auth";

declare module "firebase/auth" {
  interface ReactNativeAsyncStorage {
    getItem(key: string): Promise<string | null>;
    setItem(key: string, value: string): Promise<void>;
    removeItem(key: string): Promise<void>;
  }

  export function getReactNativePersistence(storage: ReactNativeAsyncStorage): Persistence;
}
