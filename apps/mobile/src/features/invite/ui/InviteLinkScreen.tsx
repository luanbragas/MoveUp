import { router, useLocalSearchParams } from "expo-router";
import { useEffect } from "react";
import { normalizeInviteCode } from "../domain/invite";
import { setPendingInviteCode } from "../hooks/pending-code";

/**
 * Rota do link do convite (moveup.com.br/i/CODIGO ou moveup://i/CODIGO): guarda o código e
 * segue pela entrada normal (login, cadastro, termos). A casa do aluno usa o código guardado.
 */
export function InviteLinkScreen() {
  const { code } = useLocalSearchParams();

  useEffect(() => {
    setPendingInviteCode(typeof code === "string" ? normalizeInviteCode(code) : null);
    router.replace("/");
  }, [code]);

  return null;
}
