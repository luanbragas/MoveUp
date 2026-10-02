import { Pressable, StyleSheet, Text, View } from "react-native";
import { MIN_TOUCH, spacing, typography, useColors } from "./theme";

interface Props {
  readonly label: string;
  readonly checked: boolean;
  readonly onChange: (checked: boolean) => void;
}

export function Checkbox({ label, checked, onChange }: Props) {
  const colors = useColors();
  return (
    <Pressable
      accessibilityRole="checkbox"
      accessibilityState={{ checked }}
      accessibilityLabel={label}
      onPress={() => {
        onChange(!checked);
      }}
      style={styles.row}
    >
      <View
        style={[
          styles.box,
          {
            borderColor: checked ? colors.primary : colors.border,
            backgroundColor: checked ? colors.primary : colors.background,
          },
        ]}
      >
        {checked ? <Text style={[styles.mark, { color: colors.onPrimary }]}>✓</Text> : null}
      </View>
      <Text style={[typography.body, styles.label, { color: colors.text }]}>{label}</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: "row", alignItems: "center", gap: spacing.md, minHeight: MIN_TOUCH },
  box: {
    width: 28,
    height: 28,
    borderRadius: 6,
    borderWidth: 2,
    alignItems: "center",
    justifyContent: "center",
  },
  mark: { fontSize: 18, fontWeight: "700" },
  label: { flex: 1 },
});
