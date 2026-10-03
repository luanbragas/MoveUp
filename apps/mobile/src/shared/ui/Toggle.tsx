import { Pressable, StyleSheet, Text, View } from "react-native";
import { MIN_TOUCH, palette, spacing, typography } from "./theme";

interface Props {
  readonly label: string;
  readonly description?: string;
  readonly value: boolean;
  readonly onChange: (value: boolean) => void;
}

/** Linha com interruptor (termos, privacidade, avisos). A linha inteira é tocável. */
export function Toggle({ label, description, value, onChange }: Props) {
  return (
    <Pressable
      accessibilityRole="switch"
      accessibilityLabel={label}
      accessibilityHint={description}
      accessibilityState={{ checked: value }}
      onPress={() => {
        onChange(!value);
      }}
      style={styles.row}
    >
      <View style={styles.texts}>
        <Text style={[typography.button, { color: palette.text, fontSize: 16 }]}>{label}</Text>
        {description === undefined ? null : (
          <Text style={[typography.small, { color: palette.muted }]}>{description}</Text>
        )}
      </View>
      <View style={[styles.track, { backgroundColor: value ? palette.lime : palette.line }]}>
        <View style={[styles.knob, value ? styles.on : styles.off]} />
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  row: {
    minHeight: MIN_TOUCH + 12,
    flexDirection: "row",
    alignItems: "center",
    gap: spacing.md,
    paddingVertical: spacing.sm,
  },
  texts: { flex: 1, gap: 2 },
  track: { width: 52, height: 32, borderRadius: 16, justifyContent: "center" },
  knob: { width: 26, height: 26, borderRadius: 13, position: "absolute" },
  on: { right: 3, backgroundColor: palette.onLime },
  off: { left: 3, backgroundColor: palette.muted },
});
