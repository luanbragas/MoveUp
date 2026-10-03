import { Message } from "../../../shared/ui/Message";
import { Screen } from "../../../shared/ui/Screen";
import { Skeleton } from "../../../shared/ui/Skeleton";
import { useSession } from "../hooks/use-sessions";
import { ExecutionScreen, type ExerciseInfoLite } from "./ExecutionScreen";

interface Props {
  readonly sessionId: string;
  readonly exercises: ReadonlyMap<string, ExerciseInfoLite>;
  readonly firstName: string;
  readonly onClose: () => void;
}

/** Rota da sessão: carrega do aparelho e abre a execução (ou o resumo, se já terminou). */
export function SessionScreen({ sessionId, exercises, firstName, onClose }: Props) {
  const session = useSession(sessionId);
  if (session.isPending) {
    return (
      <Screen>
        <Skeleton width="60%" height={40} />
        <Skeleton width="100%" height={220} rounded={24} />
      </Screen>
    );
  }
  if (session.data == null) {
    return (
      <Screen>
        <Message text="Treino não encontrado neste celular." />
      </Screen>
    );
  }
  return (
    <ExecutionScreen
      key={session.data.id}
      session={session.data}
      exercises={exercises}
      firstName={firstName}
      onClose={onClose}
    />
  );
}
