import { StyleSheet, Text } from "react-native";
import { spacing, typography, useColors } from "./theme";

/** Mensagem de erro (ou aviso) da tela, anunciada ao leitor de tela. */
export function Message({
  text,
  tone = "error",
}: {
  readonly text: string;
  readonly tone?: "error" | "info";
}) {
  const colors = useColors();
  return (
    <Text
      accessibilityLiveRegion="polite"
      style={[
        typography.body,
        styles.box,
        { color: tone === "error" ? colors.danger : colors.textMuted, borderColor: colors.border },
      ]}
    >
      {text}
    </Text>
  );
}

const styles = StyleSheet.create({
  box: { borderWidth: 1, borderRadius: 10, padding: spacing.md },
});
