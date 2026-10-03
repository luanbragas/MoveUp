import { router } from "expo-router";
import { useState } from "react";
import { Alert, StyleSheet, Text, View } from "react-native";
import { toAppError } from "../../../shared/lib/http";
import { Button } from "../../../shared/ui/Button";
import { errorMessage } from "../../../shared/ui/error-messages";
import { EmptyState } from "../../../shared/ui/EmptyState";
import { Message } from "../../../shared/ui/Message";
import { RoundButton } from "../../../shared/ui/RoundButton";
import { Screen } from "../../../shared/ui/Screen";
import { Segmented } from "../../../shared/ui/Segmented";
import { Skeleton } from "../../../shared/ui/Skeleton";
import { palette, typography } from "../../../shared/ui/theme";
import { inviteUrl, type ClientAction, type ClientItem } from "../domain/client";
import { useClients, useLinkCommand, type LinkCommand } from "../hooks/use-clients";
import { ClientCard } from "./ClientCard";
import { openShare } from "./share-params";
import { strings } from "./strings";

const t = strings.list;

/** Ações que pedem confirmação antes (mudam o vínculo ou invalidam o convite). */
const CONFIRMED: Readonly<Partial<Record<ClientAction, keyof typeof strings.confirm>>> = {
  "cancel-invite": "cancel-invite",
  inactivate: "inactivate",
  end: "end",
};

type Filter = "all" | "active" | "invites";

/** Lista de alunos do profissional com convite e ações por estado (SCREEN-FLOWS 2.2). */
export function ClientsScreen() {
  const clients = useClients();
  const command = useLinkCommand();
  const [filter, setFilter] = useState<Filter>("all");

  const run = (action: Exclude<ClientAction, "share">, client: ClientItem) => {
    command.mutate(
      { command: action satisfies LinkCommand, linkId: client.linkId },
      {
        onSuccess: (invitation) => {
          if (invitation !== null) {
            openShare(invitation, { name: client.name, phone: null }, "push");
          }
        },
      },
    );
  };

  const onAction = (action: ClientAction, client: ClientItem) => {
    if (action === "share") {
      if (client.pendingInvite !== null) {
        const { code, expiresAt } = client.pendingInvite;
        openShare(
          { code, expiresAt, url: inviteUrl(code) },
          { name: client.name, phone: null },
          "push",
        );
      }
      return;
    }
    const confirmKey = CONFIRMED[action];
    if (confirmKey === undefined) {
      run(action, client);
      return;
    }
    const copy = strings.confirm[confirmKey];
    Alert.alert(copy.title, `${client.name}: ${copy.message}`, [
      { text: strings.confirmBack, style: "cancel" },
      {
        text: strings.actions[action],
        style: "destructive",
        onPress: () => {
          run(action, client);
        },
      },
    ]);
  };

  const items = clients.data?.pages.flatMap((page) => page.items) ?? [];
  const invites = items.filter((client) => client.status === "pending");
  const active = items.filter((client) => client.status === "active");
  const shown = filter === "all" ? items : filter === "active" ? active : invites;
  const invite = () => {
    router.push("/clients/new");
  };

  return (
    <Screen
      header={
        <>
          <View style={styles.flex} />
          {items.length > 0 ? <RoundButton icon="plus" label={t.invite} onPress={invite} /> : null}
        </>
      }
      title={t.title}
      {...(clients.isSuccess ? { subtitle: t.count(items.length) } : {})}
      refresh={{
        refreshing: clients.isRefetching,
        onRefresh: () => {
          void clients.refetch();
        },
      }}
    >
      {command.isError ? <Message text={errorMessage(toAppError(command.error))} /> : null}
      {clients.isPending ? (
        <>
          <Skeleton width="100%" height={44} rounded={22} />
          <Skeleton width="100%" height={112} rounded={24} />
          <Skeleton width="100%" height={112} rounded={24} />
        </>
      ) : null}
      {clients.isError ? (
        <>
          <Message text={t.error} />
          <Button
            label={t.retry}
            variant="secondary"
            icon="refresh"
            onPress={() => {
              void clients.refetch();
            }}
          />
        </>
      ) : null}
      {clients.isSuccess && items.length === 0 ? (
        <EmptyState
          icon="users"
          title={t.emptyTitle}
          text={t.emptyText}
          action={<Button label={t.invite} icon="plus" onPress={invite} />}
        />
      ) : null}
      {items.length > 0 ? (
        <Segmented
          label={t.filter}
          value={filter}
          onChange={setFilter}
          options={[
            { value: "all", label: t.filters.all(items.length) },
            { value: "active", label: t.filters.active(active.length) },
            { value: "invites", label: t.filters.invites(invites.length) },
          ]}
        />
      ) : null}
      {items.length > 0 && shown.length === 0 ? (
        <Text style={[typography.body, { color: palette.muted }]}>{t.emptyFilter}</Text>
      ) : null}
      {shown.map((client) => (
        <ClientCard
          key={client.linkId}
          client={client}
          busy={command.isPending && command.variables.linkId === client.linkId}
          onAction={onAction}
        />
      ))}
      {clients.hasNextPage ? (
        <Button
          label={t.loadMore}
          variant="secondary"
          icon={null}
          loading={clients.isFetchingNextPage}
          onPress={() => {
            void clients.fetchNextPage();
          }}
        />
      ) : null}
    </Screen>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1 },
});
