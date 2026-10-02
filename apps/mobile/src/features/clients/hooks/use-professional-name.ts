import { useAuthState, useMe } from "../../auth";
import { firstName } from "../domain/client";

/** Primeiro nome do profissional logado, para a mensagem do convite (já está em cache). */
export function useProfessionalFirstName(): string | null {
  const auth = useAuthState();
  const me = useMe(auth.status === "signed-in" ? auth.user.uid : null);
  return me.data === undefined ? null : firstName(me.data.name);
}
