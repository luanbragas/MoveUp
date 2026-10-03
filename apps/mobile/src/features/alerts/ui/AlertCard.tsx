import { Pressable, StyleSheet, Text, View } from "react-native";
import { Icon, type IconName } from "../../../shared/ui/Icon";
import { MIN_TOUCH, palette, radius, spacing, typography } from "../../../shared/ui/theme";
import type { Alert, AlertType } from "../domain/alert";
import { strings } from "./strings";

const t = strings.card;

const ICON: Record<AlertType, IconName> = {
  pain_reported: "alert",
  high_effort: "trend",
  new_feedback: "chat",
  session_edited: "edit",
  inactive: "clock",
  low_adherence: "chart",
  clearance_pending: "shield",
};

const dayFormat = new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "short" });

/** "agora", "há 3 h", "ontem", "12 de out." */
export function timeAgo(date: Date, now = new Date()): string {
  const minutes = Math.floor((now.getTime() - date.getTime()) / 60_000);
  if (minutes < 1) {
    return "agora";
  }
  if (minutes < 60) {
    return `há ${String(minutes)} min`;
  }
  const hours = Math.floor(minutes / 60);
  if (hours < 24) {
    return `há ${String(hours)} h`;
  }
  return hours < 48 ? "ontem" : dayFormat.format(date);
}

interface Props {
  readonly alert: Alert;
  readonly onOpen?: (() => void) | undefined;
  readonly onResolve?: (() => void) | undefined;
  readonly onSnooze?: (() => void) | undefined;
}

/** Um alerta: barra vermelha no urgente, aluno, o que aconteceu e as ações. */
export function AlertCard({ alert, onOpen, onResolve, onSnooze }: Props) {
  const { title, detail } = strings.describe(alert.type, alert.facts);
  const urgent = alert.severity === "urgent";
  const name = alert.clientName ?? t.unknownClient;
  return (
    <View style={[styles.card, urgent ? styles.urgent : null]}>
      <Pressable
        accessibilityRole="button"
        accessibilityLabel={`${name}: ${title}. ${timeAgo(alert.createdAt)}`}
        accessibilityHint={onOpen === undefined ? undefined : t.openClient(name)}
        disabled={onOpen === undefined}
        onPress={onOpen}
        style={styles.main}
      >
        <View style={[styles.icon, urgent ? styles.iconUrgent : null]}>
          <Icon name={ICON[alert.type]} size={18} color={urgent ? palette.red : palette.lime} />
        </View>
        <View style={{ flex: 1, gap: 2 }}>
          <View style={styles.headRow}>
            <Text numberOfLines={1} style={[typography.small, styles.name]}>
              {name}
            </Text>
            <Text style={[typography.small, { color: palette.muted }]}>
              {timeAgo(alert.createdAt)}
            </Text>
          </View>
          <Text style={[typography.label, { color: urgent ? palette.red : palette.text }]}>
            {title}
          </Text>
          <Text style={[typography.small, { color: palette.textSoft }]}>{detail}</Text>
          {alert.status === "snoozed" && alert.snoozedUntil !== null ? (
            <Text style={[typography.small, { color: palette.muted }]}>
              {t.snoozedUntil(dayFormat.format(alert.snoozedUntil))}
            </Text>
          ) : null}
        </View>
      </Pressable>
      {onResolve === undefined && onSnooze === undefined ? null : (
        <View style={styles.actions}>
          {onSnooze === undefined ? null : (
            <Pressable
              accessibilityRole="button"
              accessibilityLabel={`${t.snooze}: ${name}, ${title}`}
              onPress={onSnooze}
              style={styles.action}
            >
              <Icon name="clock" size={16} color={palette.muted} />
              <Text style={[typography.label, { color: palette.muted }]}>{t.snooze}</Text>
            </Pressable>
          )}
          {onResolve === undefined ? null : (
            <Pressable
              accessibilityRole="button"
              accessibilityLabel={`${t.resolve}: ${name}, ${title}`}
              onPress={onResolve}
              style={[styles.action, styles.resolve]}
            >
              <Icon name="check" size={16} color={palette.onLime} />
              <Text style={[typography.label, { color: palette.onLime }]}>{t.resolve}</Text>
            </Pressable>
          )}
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: palette.surface,
    borderRadius: radius.lg,
    padding: spacing.md,
    gap: spacing.sm + 2,
    borderLeftWidth: 4,
    borderLeftColor: palette.surface,
  },
  urgent: { borderLeftColor: palette.red, backgroundColor: palette.redSoft },
  main: { flexDirection: "row", gap: spacing.sm + 4, alignItems: "flex-start" },
  icon: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: palette.limeDark,
    alignItems: "center",
    justifyContent: "center",
  },
  iconUrgent: { backgroundColor: "rgba(255, 59, 59, 0.16)" },
  headRow: { flexDirection: "row", alignItems: "center", gap: spacing.sm },
  name: { color: palette.textSoft, flex: 1 },
  actions: { flexDirection: "row", justifyContent: "flex-end", gap: spacing.sm },
  action: {
    flexDirection: "row",
    alignItems: "center",
    gap: 6,
    minHeight: MIN_TOUCH,
    paddingHorizontal: spacing.md,
    borderRadius: radius.pill,
    backgroundColor: palette.surface2,
  },
  resolve: { backgroundColor: palette.lime },
});
