import { Pressable, StyleSheet, Text, View } from "react-native";
import { Chip } from "../../../shared/ui/Chip";
import { fonts, palette, radius, spacing, typography } from "../../../shared/ui/theme";
import { availableActions, type ClientAction, type ClientItem } from "../domain/client";
import { strings } from "./strings";

interface Props {
  readonly client: ClientItem;
  readonly busy: boolean;
  readonly onAction: (action: ClientAction, client: ClientItem) => void;
  /** Abre o programa do aluno (pendente ou ativo). */
  readonly onOpen?: (client: ClientItem) => void;
}

const formatDate = (date: Date) => date.toLocaleDateString("pt-BR");

function initials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  return (
    (parts[0]?.[0] ?? "") + (parts.length > 1 ? (parts.at(-1)?.[0] ?? "") : "")
  ).toUpperCase();
}

/** Um aluno na lista: avatar, nome, estado do vínculo e as ações que o estado permite. */
export function ClientCard({ client, busy, onAction, onOpen }: Props) {
  const detail =
    client.status === "pending"
      ? client.pendingInvite === null
        ? strings.list.noInvite
        : strings.list.inviteExpires(formatDate(client.pendingInvite.expiresAt))
      : client.startedAt === null
        ? null
        : strings.list.since(formatDate(client.startedAt));
  const active = client.status === "active";

  return (
    <View style={[styles.card, busy ? styles.dimmed : null]}>
      <Pressable
        accessibilityRole="button"
        accessibilityLabel={client.name}
        disabled={onOpen === undefined || client.status === "inactive"}
        onPress={() => {
          onOpen?.(client);
        }}
        style={styles.head}
      >
        <View style={[styles.avatar, active ? styles.avatarActive : null]}>
          <Text style={[styles.initials, { color: active ? palette.lime : palette.muted }]}>
            {initials(client.name)}
          </Text>
        </View>
        <View style={styles.texts}>
          <Text style={[typography.label, { color: palette.text, fontSize: 16 }]} numberOfLines={1}>
            {client.name}
          </Text>
          {detail === null ? null : (
            <Text style={[typography.small, { color: palette.muted }]}>{detail}</Text>
          )}
        </View>
        <View style={[styles.status, client.status === "pending" ? styles.statusPending : null]}>
          <Text
            style={[
              typography.small,
              { color: client.status === "pending" ? palette.onLime : palette.textSoft },
            ]}
          >
            {strings.status[client.status]}
          </Text>
        </View>
      </Pressable>
      <View style={styles.actions}>
        {availableActions(client).map((action) => (
          <Chip
            key={action}
            label={strings.actions[action]}
            onPress={() => {
              if (!busy) {
                onAction(action, client);
              }
            }}
          />
        ))}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: palette.surface,
    borderRadius: radius.lg,
    padding: spacing.md,
    gap: spacing.sm + 4,
  },
  dimmed: { opacity: 0.6 },
  head: { flexDirection: "row", alignItems: "center", gap: spacing.sm + 4 },
  avatar: {
    width: 46,
    height: 46,
    borderRadius: 23,
    backgroundColor: palette.surface2,
    alignItems: "center",
    justifyContent: "center",
  },
  avatarActive: { backgroundColor: palette.limeDark },
  initials: { fontFamily: fonts.number, fontSize: 16 },
  texts: { flex: 1, gap: 2 },
  status: {
    borderRadius: radius.pill,
    paddingHorizontal: 10,
    paddingVertical: 4,
    backgroundColor: palette.surface2,
  },
  statusPending: { backgroundColor: palette.lime },
  actions: { flexDirection: "row", flexWrap: "wrap", gap: spacing.sm },
});
