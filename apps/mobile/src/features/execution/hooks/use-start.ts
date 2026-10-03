import { useMutation } from "@tanstack/react-query";
import { uuidV7 } from "../../../shared/lib/uuid-v7";
import { startSession, type PlannedInput, type Session } from "../domain/session";
import { useSaveSession } from "./use-sessions";

/** Cria a sessão no aparelho (ids UUIDv7) com as séries pré-preenchidas e grava. */
export function useStartSession() {
  const save = useSaveSession();
  return useMutation({
    networkMode: "always",
    mutationFn: async ({
      planned,
      performedBy,
    }: {
      planned: PlannedInput;
      performedBy: Session["performedBy"];
    }) => save.mutateAsync(startSession(planned, performedBy, new Date(), () => uuidV7())),
  });
}
