import { router, useLocalSearchParams } from "expo-router";
import { isMuscleCode, useExercisesByMuscle } from "../../exercise-library";
import { SessionScreen, useSession } from "../../execution";

/**
 * Rota /session/[id] do personal: o treino presencial registrado pelo aluno, com a biblioteca
 * dos músculos do treino (como fazer e troca por outro exercício do mesmo músculo).
 */
export function PresencialSessionScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const session = useSession(id);
  const muscles = (session.data?.exercises ?? []).map((e) => e.primaryMuscle).filter(isMuscleCode);
  const library = useExercisesByMuscle(muscles);
  return (
    <SessionScreen
      sessionId={id}
      exercises={library}
      firstName=""
      onClose={() => {
        router.back();
      }}
    />
  );
}
