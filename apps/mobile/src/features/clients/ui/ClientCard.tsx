import { Pressable, StyleSheet, Text, View } from "react-native";
import { MIN_TOUCH, spacing, typography, useColors } from "../../../shared/ui/theme";
import { availableActions, type ClientAction, type ClientItem } from "../domain/client";
import { strings } from "./strings";

interface Props {
  readonly client: ClientItem;
  readonly busy: boolean;
  readonly onAction: (action: ClientAction, client: ClientItem) => void;
}

const formatDate = (date: Date) => date.toLocaleDateString("pt-BR");

/** Um aluno na lista: nome, estado do vínculo e as ações que o estado permite. */
export function ClientCard({ client, busy, onAction }: Props) {
  const colors = useColors();
  const detail =
    client.status === "pending"
      ? client.pendingInvite === null
        ? strings.list.noInvite
        : strings.list.inviteExpires(formatDate(client.pendingInvite.expiresAt))
      : client.startedAt === null
        ? null
        : strings.list.since(formatDate(client.startedAt));

  return (
    <View style={[styles.card, { borderColor: colors.border, backgroundColor: colors.surface }]}>
      <View style={styles.header}>
        <Text style={[typography.label, styles.name, { color: colors.text }]}>{client.name}</Text>
        <Text style={[typography.small, { color: colors.textMuted }]}>
          {strings.status[client.status]}
        </Text>
      </View>
      {detail === null ? null : (
        <Text style={[typography.small, { color: colors.textMuted }]}>{detail}</Text>
      )}
      <View style={styles.actions}>
        {availableActions(client).map((action) => (
          <Pressable
            key={action}
            accessibilityRole="button"
            accessibilityLabel={`${strings.actions[action]}: ${client.name}`}
            accessibilityState={{ disabled: busy }}
            disabled={busy}
            onPress={() => {
              onAction(action, client);
            }}
            style={({ pressed }) => [
              styles.action,
              { borderColor: action === "end" ? colors.danger : colors.border },
              (pressed || busy) && styles.dimmed,
            ]}
          >
            <Text
              style={[typography.small, { color: action === "end" ? colors.danger : colors.text }]}
            >
              {strings.actions[action]}
            </Text>
          </Pressable>
        ))}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  card: { borderWidth: 1, borderRadius: 12, padding: spacing.md, gap: spacing.sm },
  header: { flexDirection: "row", alignItems: "center", gap: spacing.sm },
  name: { flex: 1 },
  actions: { flexDirection: "row", flexWrap: "wrap", gap: spacing.sm },
  action: {
    minHeight: MIN_TOUCH,
    borderWidth: 1,
    borderRadius: 10,
    paddingHorizontal: spacing.md,
    justifyContent: "center",
  },
  dimmed: { opacity: 0.6 },
});
