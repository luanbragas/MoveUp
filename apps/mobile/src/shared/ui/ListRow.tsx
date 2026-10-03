import { Pressable, StyleSheet, Text, View } from "react-native";
import { Icon, type IconName } from "./Icon";
import { MIN_TOUCH, palette, spacing, typography } from "./theme";

interface Props {
  readonly title: string;
  readonly subtitle?: string;
  readonly icon?: IconName;
  readonly onPress: () => void;
  /** Ação destrutiva (sair perdendo dados, excluir). */
  readonly danger?: boolean;
  /** Última linha do grupo: sem separador embaixo. */
  readonly last?: boolean;
}

/** Linha de lista tocável inteira, com ícone, título, apoio e seta. */
export function ListRow({ title, subtitle, icon, onPress, danger = false, last = false }: Props) {
  const tint = danger ? palette.red : palette.lime;
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={subtitle === undefined ? title : `${title}. ${subtitle}`}
      onPress={onPress}
      style={({ pressed }) => [
        styles.row,
        last ? null : styles.separator,
        pressed ? styles.pressed : null,
      ]}
    >
      {icon === undefined ? null : (
        <View
          style={[styles.iconBox, { backgroundColor: danger ? palette.redSoft : palette.surface2 }]}
        >
          <Icon name={icon} size={20} color={tint} strokeWidth={2.2} />
        </View>
      )}
      <View style={styles.texts}>
        <Text
          style={[typography.button, { fontSize: 16, color: danger ? palette.red : palette.text }]}
        >
          {title}
        </Text>
        {subtitle === undefined ? null : (
          <Text style={[typography.small, { color: palette.muted }]}>{subtitle}</Text>
        )}
      </View>
      <Icon name="chevron" size={18} color={palette.muted} strokeWidth={2.4} />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  row: {
    minHeight: MIN_TOUCH + 18,
    flexDirection: "row",
    alignItems: "center",
    gap: spacing.md - 2,
    paddingVertical: spacing.sm + 4,
  },
  separator: { borderBottomWidth: 1, borderBottomColor: palette.line },
  iconBox: {
    width: 42,
    height: 42,
    borderRadius: 21,
    alignItems: "center",
    justifyContent: "center",
  },
  texts: { flex: 1, gap: 2 },
  pressed: { opacity: 0.7 },
});
