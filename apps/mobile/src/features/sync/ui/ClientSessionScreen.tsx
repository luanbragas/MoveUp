import { router, useLocalSearchParams } from "expo-router";
import { useAuthState, useMe } from "../../auth";
import { SessionScreen } from "../../execution";
import { usePlannedSnapshot } from "../hooks/use-planned";

/** Rota /session/[id] do aluno: a execução com a biblioteca baixada (como fazer, troca). */
export function ClientSessionScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const snapshot = usePlannedSnapshot();
  const auth = useAuthState();
  const me = useMe(auth.status === "signed-in" ? auth.user.uid : null);
  return (
    <SessionScreen
      sessionId={id}
      exercises={snapshot.data?.exercises ?? new Map()}
      firstName={me.data?.name.trim().split(/\s+/)[0] ?? ""}
      onClose={() => {
        router.back();
      }}
    />
  );
}
