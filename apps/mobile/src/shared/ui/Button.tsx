import { ActivityIndicator, Pressable, StyleSheet, Text, View } from "react-native";
import { Icon, type IconName } from "./Icon";
import { MIN_TOUCH, palette, radius, spacing, typography } from "./theme";

export type ButtonVariant = "primary" | "secondary" | "danger" | "danger-soft" | "ghost";

interface Props {
  readonly label: string;
  readonly onPress: () => void;
  readonly variant?: ButtonVariant;
  readonly loading?: boolean;
  readonly disabled?: boolean;
  /**
   * Ícone do botão. No principal fica no círculo à direita (padrão: seta);
   * nos outros, antes do texto. `null` tira o ícone.
   */
  readonly icon?: IconName | null;
  /** Botão principal sobre uma superfície lima: vira pílula escura com círculo lima. */
  readonly onLime?: boolean;
}

export function Button({
  label,
  onPress,
  variant = "primary",
  loading = false,
  disabled = false,
  icon,
  onLime = false,
}: Props) {
  const inactive = disabled || loading;
  const primary = variant === "primary";
  const look = primary ? (onLime ? LOOK.primaryOnLime : LOOK.primary) : LOOK[variant];
  const shownIcon = icon === undefined ? (primary ? "arrow" : null) : icon;

  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={label}
      accessibilityState={{ disabled: inactive, busy: loading }}
      disabled={inactive}
      onPress={onPress}
      style={({ pressed }) => [
        styles.base,
        primary ? styles.primary : styles.other,
        { backgroundColor: disabled ? palette.surface2 : look.background },
        look.border ? styles.highlight : null,
        primary && !onLime && !disabled ? styles.shadow : null,
        pressed && !inactive ? styles.pressed : null,
      ]}
    >
      {loading ? (
        <View style={styles.center}>
          <ActivityIndicator color={look.text} />
        </View>
      ) : primary ? (
        <>
          <Text
            style={[
              typography.button,
              styles.primaryLabel,
              { color: disabled ? palette.muted : look.text },
            ]}
            numberOfLines={1}
          >
            {label}
          </Text>
          {shownIcon !== null && !disabled ? (
            <View style={[styles.circle, { backgroundColor: look.circle }]}>
              <Icon name={shownIcon} size={22} color={look.circleIcon} strokeWidth={2.6} />
            </View>
          ) : null}
        </>
      ) : (
        <View style={styles.center}>
          {shownIcon !== null ? (
            <Icon name={shownIcon} size={20} color={look.text} strokeWidth={2.4} />
          ) : null}
          <Text
            style={[typography.button, { color: disabled ? palette.muted : look.text }]}
            numberOfLines={1}
          >
            {label}
          </Text>
        </View>
      )}
    </Pressable>
  );
}

interface Look {
  readonly background: string;
  readonly text: string;
  readonly circle: string;
  readonly circleIcon: string;
  readonly border: boolean;
}

const LOOK: Record<"primary" | "primaryOnLime" | Exclude<ButtonVariant, "primary">, Look> = {
  primary: {
    background: palette.lime,
    text: palette.onLime,
    circle: palette.onLime,
    circleIcon: palette.lime,
    border: false,
  },
  primaryOnLime: {
    background: palette.onLime,
    text: palette.text,
    circle: palette.lime,
    circleIcon: palette.onLime,
    border: false,
  },
  secondary: {
    background: "#1E1E22",
    text: palette.text,
    circle: "",
    circleIcon: "",
    border: true,
  },
  danger: {
    background: palette.red,
    text: palette.onLime,
    circle: "",
    circleIcon: "",
    border: false,
  },
  "danger-soft": {
    background: palette.redSoft,
    text: palette.red,
    circle: "",
    circleIcon: "",
    border: false,
  },
  ghost: {
    background: "transparent",
    text: palette.muted,
    circle: "",
    circleIcon: "",
    border: false,
  },
};

const styles = StyleSheet.create({
  base: {
    minHeight: MIN_TOUCH + 12,
    borderRadius: radius.pill,
    flexDirection: "row",
    alignItems: "center",
  },
  primary: {
    minHeight: 64,
    paddingLeft: spacing.lg + 2,
    paddingRight: spacing.sm,
    justifyContent: "space-between",
  },
  other: { paddingHorizontal: spacing.lg, justifyContent: "center" },
  primaryLabel: { flexShrink: 1 },
  center: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: 10,
    flex: 1,
  },
  circle: {
    width: 48,
    height: 48,
    borderRadius: 24,
    alignItems: "center",
    justifyContent: "center",
  },
  highlight: { borderTopWidth: 1, borderColor: "rgba(255, 255, 255, 0.08)" },
  shadow: {
    shadowColor: "#000",
    shadowOpacity: 0.45,
    shadowRadius: 12,
    shadowOffset: { width: 0, height: 10 },
    elevation: 8,
  },
  pressed: { opacity: 0.8, transform: [{ scale: 0.98 }] },
});
