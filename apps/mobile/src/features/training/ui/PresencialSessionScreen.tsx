import { router, useLocalSearchParams } from "expo-router";
import { SessionScreen } from "../../execution";

const NO_LIBRARY = new Map();

/** Rota /session/[id] do personal: o treino presencial registrado pelo aluno. */
export function PresencialSessionScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  return (
    <SessionScreen
      sessionId={id}
      exercises={NO_LIBRARY}
      firstName=""
      onClose={() => {
        router.back();
      }}
    />
  );
}
