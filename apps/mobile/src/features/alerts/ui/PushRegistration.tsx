import { usePushRegistration } from "../hooks/use-alerts";

/** Montado nas abas do profissional: registra o aparelho para push ao abrir o app. */
export function PushRegistration() {
  usePushRegistration();
  return null;
}
