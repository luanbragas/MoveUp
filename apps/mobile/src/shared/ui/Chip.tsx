import { Pressable, StyleSheet, Text } from "react-native";
import { Icon, type IconName } from "./Icon";
import { MIN_TOUCH, palette, radius, spacing, typography } from "./theme";

interface Props {
  readonly label: string;
  readonly selected?: boolean;
  readonly onPress?: () => void;
  readonly icon?: IconName;
  /** Como o leitor de tela anuncia: filtro/opção de grupo ("radio") ou botão simples. */
  readonly role?: "radio" | "button";
}

/** Pílula de escolha ou filtro. Seleção é sempre lima. */
export function Chip({ label, selected = false, onPress, icon, role = "button" }: Props) {
  return (
    <Pressable
      accessibilityRole={role}
      accessibilityLabel={label}
      accessibilityState={role === "radio" ? { checked: selected } : { selected }}
      onPress={onPress}
      disabled={onPress === undefined}
      hitSlop={4}
      style={({ pressed }) => [
        styles.chip,
        { backgroundColor: selected ? palette.lime : palette.surface },
        pressed ? styles.pressed : null,
      ]}
    >
      {icon === undefined ? null : (
        <Icon
          name={icon}
          size={16}
          color={selected ? palette.onLime : palette.lime}
          strokeWidth={2.6}
        />
      )}
      <Text style={[typography.label, { color: selected ? palette.onLime : palette.text }]}>
        {label}
      </Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  chip: {
    minHeight: MIN_TOUCH - 8,
    paddingHorizontal: spacing.md,
    borderRadius: radius.pill,
    flexDirection: "row",
    alignItems: "center",
    gap: 6,
  },
  pressed: { opacity: 0.8 },
});
