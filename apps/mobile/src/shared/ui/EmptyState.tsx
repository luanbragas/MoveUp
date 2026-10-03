import type { ReactNode } from "react";
import { StyleSheet, Text, View } from "react-native";
import { Icon, type IconName } from "./Icon";
import { palette, radius, spacing, typography } from "./theme";

interface Props {
  readonly icon: IconName;
  readonly title: string;
  readonly text: string;
  /** Próximo passo (botão), quando houver. */
  readonly action?: ReactNode;
}

/**
 * Lista ou área ainda vazia: diz o que vai aparecer ali e o próximo passo. Contorno tracejado para
 * não parecer conteúdo de verdade.
 */
export function EmptyState({ icon, title, text, action }: Props) {
  return (
    <View style={styles.box}>
      <View style={styles.icon}>
        <Icon name={icon} size={26} color={palette.lime} />
      </View>
      <Text accessibilityRole="header" style={[typography.headline, { color: palette.text }]}>
        {title}
      </Text>
      <Text style={[typography.body, { color: palette.textSoft }]}>{text}</Text>
      {action}
    </View>
  );
}

const styles = StyleSheet.create({
  box: {
    backgroundColor: palette.surface,
    borderRadius: radius.lg,
    padding: spacing.lg,
    gap: spacing.sm + 4,
    borderWidth: 1,
    borderColor: palette.line,
    borderStyle: "dashed",
  },
  icon: {
    width: 52,
    height: 52,
    borderRadius: 26,
    backgroundColor: palette.limeDark,
    alignItems: "center",
    justifyContent: "center",
  },
});
