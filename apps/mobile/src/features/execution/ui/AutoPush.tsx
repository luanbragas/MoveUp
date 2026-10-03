import { useAutoPush } from "../hooks/use-sessions";

/** Envia o treino registrado ao abrir, ao voltar para o app e quando a internet volta. */
export function AutoPush() {
  useAutoPush();
  return null;
}
