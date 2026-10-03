import { Pressable, StyleSheet, Text, View } from "react-native";
import { Icon } from "./Icon";
import { MIN_TOUCH, palette, radius, spacing, typography } from "./theme";

interface Props {
  readonly label: string;
  readonly checked: boolean;
  readonly onChange: (checked: boolean) => void;
  /** Texto de apoio abaixo (ex.: para que serve o consentimento). */
  readonly description?: string;
}

/** Caixa de seleção para aceite explícito (consentimentos): começa sempre desmarcada. */
export function Checkbox({ label, checked, onChange, description }: Props) {
  return (
    <Pressable
      accessibilityRole="checkbox"
      accessibilityState={{ checked }}
      accessibilityLabel={label}
      accessibilityHint={description}
      onPress={() => {
        onChange(!checked);
      }}
      style={styles.row}
    >
      <View style={[styles.box, checked ? styles.checked : styles.unchecked]}>
        {checked ? <Icon name="check" size={18} color={palette.onLime} strokeWidth={3.2} /> : null}
      </View>
      <View style={styles.texts}>
        <Text style={[typography.body, { color: palette.text }]}>{label}</Text>
        {description === undefined ? null : (
          <Text style={[typography.small, { color: palette.muted }]}>{description}</Text>
        )}
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: "row",
    alignItems: "flex-start",
    gap: spacing.md - 2,
    minHeight: MIN_TOUCH,
    paddingVertical: 6,
  },
  box: {
    width: 32,
    height: 32,
    borderRadius: radius.sm - 2,
    alignItems: "center",
    justifyContent: "center",
  },
  checked: { backgroundColor: palette.lime },
  unchecked: { borderWidth: 2, borderColor: palette.muted },
  texts: { flex: 1, gap: 2, paddingTop: 4 },
});
