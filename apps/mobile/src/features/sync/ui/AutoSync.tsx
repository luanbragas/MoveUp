import { useAutoSync } from "../hooks/use-planned";

/** Baixa o planejado ao abrir o app do aluno e sempre que ele volta para a frente. */
export function AutoSync() {
  useAutoSync();
  return null;
}
