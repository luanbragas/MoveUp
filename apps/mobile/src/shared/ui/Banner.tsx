import { Pressable, StyleSheet, Text, View } from "react-native";
import { Icon, type IconName } from "./Icon";
import { MIN_TOUCH, palette, radius, spacing, typography } from "./theme";

interface Props {
  readonly text: string;
  readonly icon?: IconName;
  readonly action?: { readonly label: string; readonly onPress: () => void };
}

/** Aviso discreto no topo (sem internet, falha ao enviar). Não é erro grave: o app segue funcionando. */
export function Banner({ text, icon = "cloudOff", action }: Props) {
  return (
    <View accessibilityRole="alert" accessibilityLiveRegion="polite" style={styles.box}>
      <Icon name={icon} size={20} color={palette.textSoft} strokeWidth={2.2} />
      <Text style={[typography.label, styles.text]}>{text}</Text>
      {action === undefined ? null : (
        <Pressable
          accessibilityRole="button"
          accessibilityLabel={action.label}
          onPress={action.onPress}
          style={styles.action}
          hitSlop={8}
        >
          <Text style={[typography.label, { color: palette.lime }]}>{action.label}</Text>
        </Pressable>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  box: {
    flexDirection: "row",
    alignItems: "center",
    gap: 10,
    paddingVertical: spacing.sm + 4,
    paddingHorizontal: spacing.md - 2,
    borderRadius: radius.md,
    backgroundColor: palette.surface2,
    borderTopWidth: 1,
    borderTopColor: "rgba(255, 255, 255, 0.05)",
  },
  text: { flex: 1, color: palette.textSoft },
  action: { minHeight: MIN_TOUCH - 8, justifyContent: "center" },
});
