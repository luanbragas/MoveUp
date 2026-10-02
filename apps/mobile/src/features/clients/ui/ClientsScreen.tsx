import { router } from "expo-router";
import type { ReactNode } from "react";
import { ActivityIndicator, Alert } from "react-native";
import { toAppError } from "../../../shared/lib/http";
import { Button } from "../../../shared/ui/Button";
import { errorMessage } from "../../../shared/ui/error-messages";
import { Message } from "../../../shared/ui/Message";
import { Screen } from "../../../shared/ui/Screen";
import { useColors } from "../../../shared/ui/theme";
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

interface Props {
  /** Rodapé da tela (ex.: sair), até existir a aba de ajustes. */
  readonly footer?: ReactNode;
}

/** Lista de alunos do profissional com convite e ações por estado (SCREEN-FLOWS 2.2). */
export function ClientsScreen({ footer }: Props) {
  const colors = useColors();
  const clients = useClients();
  const command = useLinkCommand();

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

  return (
    <Screen
      title={t.title}
      {...(clients.isSuccess ? { subtitle: t.count(items.length) } : {})}
      refresh={{
        refreshing: clients.isRefetching,
        onRefresh: () => {
          void clients.refetch();
        },
      }}
    >
      <Button
        label={t.invite}
        onPress={() => {
          router.push("/clients/new");
        }}
      />
      {command.isError ? <Message text={errorMessage(toAppError(command.error))} /> : null}
      {clients.isPending ? (
        <ActivityIndicator accessibilityLabel="Carregando" color={colors.primary} />
      ) : null}
      {clients.isError ? (
        <>
          <Message text={t.error} />
          <Button
            label={t.retry}
            variant="secondary"
            onPress={() => {
              void clients.refetch();
            }}
          />
        </>
      ) : null}
      {clients.isSuccess && items.length === 0 ? <Message tone="info" text={t.empty} /> : null}
      {items.map((client) => (
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
          loading={clients.isFetchingNextPage}
          onPress={() => {
            void clients.fetchNextPage();
          }}
        />
      ) : null}
      {footer}
    </Screen>
  );
}
