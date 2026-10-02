// Declara as variáveis EXPO_PUBLIC_* lidas pelo app. O Expo só injeta o valor quando
// o acesso é `process.env.EXPO_PUBLIC_X` (com ponto); declarar aqui permite isso sem
// desligar noPropertyAccessFromIndexSignature. Validadas com Zod em env.ts.
declare namespace NodeJS {
  interface ProcessEnv {
    readonly EXPO_PUBLIC_API_URL?: string;
    readonly EXPO_PUBLIC_FIREBASE_API_KEY?: string;
    readonly EXPO_PUBLIC_FIREBASE_PROJECT_ID?: string;
    readonly EXPO_PUBLIC_FIREBASE_APP_ID?: string;
  }
}
