import { router, useLocalSearchParams } from "expo-router";
import { useState } from "react";
import { Text, View } from "react-native";
import { Button } from "../../../shared/ui/Button";
import { EmptyState } from "../../../shared/ui/EmptyState";
import { ListRow } from "../../../shared/ui/ListRow";
import { Message } from "../../../shared/ui/Message";
import { RoundButton } from "../../../shared/ui/RoundButton";
import { Screen } from "../../../shared/ui/Screen";
import { Skeleton } from "../../../shared/ui/Skeleton";
import { palette, spacing, typography } from "../../../shared/ui/theme";
import { act, durationSeconds, volumeKg, type Session } from "../domain/session";
import { useHistory, useSaveSession, useSession } from "../hooks/use-sessions";
import { SetTable } from "./ExecutionScreen";
import { strings } from "./strings";

const t = strings.history;
const dateFormat = new Intl.DateTimeFormat("pt-BR", {
  weekday: "short",
  day: "2-digit",
  month: "2-digit",
});

function subtitle(session: Session): string {
  const minutes = Math.round((durationSeconds(session) ?? 0) / 60);
  const parts = [
    dateFormat.format(new Date(session.startedAt)),
    `${String(minutes)} min`,
    `${String(volumeKg(session)).replace(".", ",")} kg`,
  ];
  if (session.status === "partial") {
    parts.push(t.partial);
  }
  if (session.syncStatus === "pending") {
    parts.push(t.pending);
  }
  return parts.join(" · ");
}

/** Aba Histórico do aluno: treinos feitos (do aparelho), com o que ainda não foi enviado. */
export function HistoryScreen() {
  const history = useHistory();
  const items = history.data ?? [];
  return (
    <Screen
      title={t.title}
      refresh={{
        refreshing: history.isRefetching,
        onRefresh: () => {
          void history.refetch();
        },
      }}
    >
      {history.isPending ? (
        <>
          <Skeleton width="100%" height={64} rounded={24} />
          <Skeleton width="100%" height={64} rounded={24} />
        </>
      ) : null}
      {history.isSuccess && items.length === 0 ? (
        <EmptyState icon="clock" title={t.emptyTitle} text={t.emptyText} />
      ) : null}
      {items.map((session, index) => (
        <ListRow
          key={session.id}
          title={session.workoutName}
          subtitle={subtitle(session)}
          icon={session.syncStatus === "pending" ? "cloudOff" : "check"}
          last={index === items.length - 1}
          onPress={() => {
            router.push({ pathname: "/history/[id]", params: { id: session.id } });
          }}
        />
      ))}
    </Screen>
  );
}

/** Rota /history/[id]: o treino feito, com correção das séries (marca edição pós-fim). */
export function SessionDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const stored = useSession(id);
  const back = (
    <RoundButton
      icon="back"
      label={t.back}
      onPress={() => {
        router.back();
      }}
    />
  );
  if (stored.data == null) {
    return (
      <Screen header={back}>
        {stored.isPending ? <Skeleton width="100%" height={200} rounded={24} /> : null}
      </Screen>
    );
  }
  return <SessionEditor key={stored.data.clientUpdatedAt} session={stored.data} header={back} />;
}

function SessionEditor({
  session: initial,
  header,
}: {
  readonly session: Session;
  readonly header: React.ReactNode;
}) {
  const [session, setSession] = useState(initial);
  const [dirty, setDirty] = useState(false);
  const save = useSaveSession();

  return (
    <Screen
      header={header}
      title={session.workoutName}
      subtitle={subtitle(session)}
      footer={
        dirty ? (
          <Button
            label={t.save}
            icon="check"
            loading={save.isPending}
            onPress={() => {
              save.mutate(session, {
                onSuccess: () => {
                  setDirty(false);
                },
              });
            }}
          />
        ) : undefined
      }
    >
      {save.isSuccess && !dirty ? <Message tone="info" text={t.edited} /> : null}
      {session.exercises.map((exercise) => (
        <View key={exercise.id} style={{ gap: spacing.sm }}>
          <Text style={[typography.headline, { color: palette.text }]}>{exercise.name}</Text>
          {exercise.status === "skipped" ? (
            <Text style={[typography.small, { color: palette.muted }]}>{strings.run.skipped}</Text>
          ) : (
            <SetTable
              exercise={exercise}
              onChange={(set, values) => {
                setSession(act.setSet(session, exercise.id, set.id, values, new Date()));
                setDirty(true);
              }}
              onConfirm={(set) => {
                setSession(
                  act.setSet(
                    session,
                    exercise.id,
                    set.id,
                    { completed: !set.completed },
                    new Date(),
                  ),
                );
                setDirty(true);
              }}
            />
          )}
        </View>
      ))}
    </Screen>
  );
}
