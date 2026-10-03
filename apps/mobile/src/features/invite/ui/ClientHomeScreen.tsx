import { router } from "expo-router";
import { useState } from "react";
import { Alert, StyleSheet, Text, View } from "react-native";
import { toAppError } from "../../../shared/lib/http";
import { Button } from "../../../shared/ui/Button";
import { Chevrons } from "../../../shared/ui/Chevrons";
import { errorMessage } from "../../../shared/ui/error-messages";
import { EmptyState } from "../../../shared/ui/EmptyState";
import { Message } from "../../../shared/ui/Message";
import { Screen } from "../../../shared/ui/Screen";
import { Skeleton } from "../../../shared/ui/Skeleton";
import { TextLink } from "../../../shared/ui/TextLink";
import { Title } from "../../../shared/ui/Title";
import { palette, radius, spacing, typography } from "../../../shared/ui/theme";
import { AnamnesisBanner } from "../../anamnesis";
import { useAuthState, useMe } from "../../auth";
import { ProgramWorkouts, TodayCard, usePlannedSnapshot, useSyncPlanned } from "../../sync";
import { currentLink, type MyLink } from "../domain/invite";
import { useEndMyLink, useMyLinks } from "../hooks/use-invite";
import { InviteFlow } from "./InviteFlow";
import { strings } from "./strings";

function firstName(name: string | undefined): string {
  return name?.trim().split(/\s+/)[0] ?? "";
}

/**
 * Aba Hoje do aluno nesta fase: sem vínculo ativo, o convite (SCREEN-FLOWS 1.2: no MVP o aluno não
 * usa o app sem personal); com vínculo, a espera pelo primeiro treino e o card do personal.
 */
export function ClientHomeScreen() {
  const auth = useAuthState();
  const me = useMe(auth.status === "signed-in" ? auth.user.uid : null);
  const links = useMyLinks();
  const planned = usePlannedSnapshot();
  const sync = useSyncPlanned();
  const [today] = useState(() => new Date());
  const [welcomed, setWelcomed] = useState<string | null>(null);
  const link = links.data === undefined ? null : currentLink(links.data);
  const name = firstName(me.data?.name);

  if (welcomed !== null) {
    const t = strings.accepted;
    return (
      <Screen
        footer={
          <Button
            label={t.go}
            onPress={() => {
              setWelcomed(null);
              // depois do aceite vem a anamnese (SCREEN-FLOWS 1.2); dá para terminar depois
              router.push("/anamnesis");
            }}
          />
        }
      >
        <View style={styles.hero}>
          <Chevrons size={96} count={3} />
          <Title size={44}>{t.title(name)}</Title>
          <Text style={[typography.body, { color: palette.textSoft }]}>{t.text(welcomed)}</Text>
        </View>
      </Screen>
    );
  }

  if (links.isPending) {
    return (
      <Screen>
        <Skeleton width="60%" height={80} />
        <Skeleton width="100%" height={140} rounded={24} />
        <Skeleton width="100%" height={88} rounded={24} />
      </Screen>
    );
  }
  if (links.isError) {
    return (
      <Screen
        title={strings.home.greeting(name)}
        footer={
          <Button
            label={strings.home.retry}
            variant="secondary"
            icon="refresh"
            onPress={() => {
              void links.refetch();
            }}
          />
        }
      >
        <Message text={strings.home.error} />
      </Screen>
    );
  }
  // pausado pelo personal continua sendo o personal dele: mostra a casa com o aviso no card
  if (link === null || link.status === "pending") {
    return <InviteFlow onAccepted={setWelcomed} />;
  }

  return (
    <Screen
      title={strings.home.greeting(name)}
      refresh={{
        refreshing: links.isRefetching,
        onRefresh: () => {
          void links.refetch();
          sync.mutate();
        },
      }}
    >
      <AnamnesisBanner />
      {planned.data?.program != null && planned.data.workouts.length > 0 ? (
        <>
          <TodayCard snapshot={planned.data} date={today} />
          <ProgramWorkouts snapshot={planned.data} />
        </>
      ) : (
        <EmptyState
          icon="dumbbell"
          title={strings.home.emptyTitle}
          text={strings.home.emptyText(link.professionalName)}
        />
      )}
      <LinkCard link={link} />
    </Screen>
  );
}

function LinkCard({ link }: { readonly link: MyLink }) {
  const end = useEndMyLink();
  const t = strings.link;

  return (
    <View style={styles.card}>
      <Text style={[typography.small, { color: palette.muted }]}>{t.yourProfessional}</Text>
      <Text style={[typography.headline, { color: palette.text }]}>{link.professionalName}</Text>
      <Text style={[typography.body, { color: palette.muted }]}>{link.organizationName}</Text>
      {link.startedAt === null ? null : (
        <Text style={[typography.small, { color: palette.muted }]}>
          {t.since(link.startedAt.toLocaleDateString("pt-BR"))}
        </Text>
      )}
      {link.status === "inactive" ? <Message tone="info" text={t.paused} /> : null}
      {end.isError ? <Message text={errorMessage(toAppError(end.error))} /> : null}
      <TextLink
        label={t.end}
        onPress={() => {
          Alert.alert(t.confirmTitle, t.confirmMessage(link.professionalName), [
            { text: t.confirmBack, style: "cancel" },
            {
              text: t.end,
              style: "destructive",
              onPress: () => {
                end.mutate(link.linkId);
              },
            },
          ]);
        }}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  hero: { gap: spacing.lg, marginTop: spacing.xl },

  card: {
    backgroundColor: palette.surface,
    borderRadius: radius.lg,
    padding: spacing.md,
    gap: spacing.xs,
  },
});
