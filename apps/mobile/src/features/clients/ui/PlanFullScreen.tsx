import { router } from "expo-router";
import { useState } from "react";
import { Alert, Pressable, StyleSheet, Text, View } from "react-native";
import { toAppError } from "../../../shared/lib/http";
import { Button } from "../../../shared/ui/Button";
import { errorMessage } from "../../../shared/ui/error-messages";
import { Message } from "../../../shared/ui/Message";
import { RoundButton } from "../../../shared/ui/RoundButton";
import { Screen } from "../../../shared/ui/Screen";
import { Title } from "../../../shared/ui/Title";
import { fonts, palette, spacing, typography } from "../../../shared/ui/theme";
import type { ClientItem, Seats } from "../domain/client";
import { useClients, useLinkCommand } from "../hooks/use-clients";
import { strings } from "./strings";

const t = strings.full;

function initials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  return (
    (parts[0]?.[0] ?? "") + (parts.length > 1 ? (parts.at(-1)?.[0] ?? "") : "")
  ).toUpperCase();
}

interface Props {
  readonly seats: Seats;
  /** Vaga liberada: volta ao formulário do convite. */
  readonly onReleased: () => void;
}

/**
 * Plano sem vaga (design 2.3): as vagas ocupadas pelos alunos ativos e a saída de liberar a de quem
 * parou (inativar não apaga nada; dá para reativar depois). Planos maiores chegam na Fase 8.
 */
export function PlanFullScreen({ seats, onReleased }: Props) {
  const clients = useClients();
  const command = useLinkCommand();
  const [picked, setPicked] = useState<ClientItem | null>(null);
  const active = (clients.data?.pages.flatMap((page) => page.items) ?? []).filter(
    (client) => client.status === "active",
  );

  const release = () => {
    if (picked === null) {
      return;
    }
    const copy = strings.confirm.inactivate;
    Alert.alert(copy.title, `${picked.name}: ${copy.message}`, [
      { text: strings.confirmBack, style: "cancel" },
      {
        text: strings.actions.inactivate,
        style: "destructive",
        onPress: () => {
          command.mutate(
            { command: "inactivate", linkId: picked.linkId },
            { onSuccess: onReleased },
          );
        },
      },
    ]);
  };

  return (
    <Screen
      header={
        <RoundButton
          icon="close"
          label={t.close}
          onPress={() => {
            router.back();
          }}
        />
      }
      footer={
        <>
          <Button
            label={picked === null ? t.releaseHint : t.release(picked.name)}
            disabled={picked === null}
            loading={command.isPending}
            onPress={release}
          />
          <Text style={[typography.small, styles.plans]}>{t.plans}</Text>
        </>
      }
    >
      <Title size={48}>{t.title}</Title>
      <Text style={[typography.body, { color: palette.textSoft }]}>{t.text(seats.limit)}</Text>
      {active.length > 0 ? (
        <View accessibilityRole="radiogroup" accessibilityLabel={t.pick} style={styles.grid}>
          {active.map((client) => {
            const selected = picked?.linkId === client.linkId;
            return (
              <Pressable
                key={client.linkId}
                accessibilityRole="radio"
                accessibilityLabel={client.name}
                accessibilityState={{ checked: selected }}
                onPress={() => {
                  setPicked(selected ? null : client);
                }}
                style={styles.seat}
              >
                <View style={[styles.avatar, selected ? styles.avatarPicked : null]}>
                  <Text
                    style={[styles.initials, { color: selected ? palette.onLime : palette.text }]}
                  >
                    {initials(client.name)}
                  </Text>
                </View>
                <Text
                  numberOfLines={1}
                  style={[typography.small, { color: selected ? palette.lime : palette.muted }]}
                >
                  {client.name.trim().split(/\s+/)[0]}
                </Text>
              </Pressable>
            );
          })}
        </View>
      ) : null}
      {command.isError ? <Message text={errorMessage(toAppError(command.error))} /> : null}
    </Screen>
  );
}

const styles = StyleSheet.create({
  grid: { flexDirection: "row", flexWrap: "wrap", gap: spacing.md, marginTop: spacing.sm },
  seat: { width: 64, alignItems: "center", gap: 6 },
  avatar: {
    width: 56,
    height: 56,
    borderRadius: 28,
    backgroundColor: palette.surface2,
    alignItems: "center",
    justifyContent: "center",
  },
  avatarPicked: { backgroundColor: palette.lime },
  initials: { fontFamily: fonts.number, fontSize: 17 },
  plans: { color: palette.muted, textAlign: "center" },
});
