import { StyleSheet, Text, View } from "react-native";
import { Icon } from "./Icon";
import { palette, radius, spacing, typography } from "./theme";

/** Mensagem da tela, anunciada ao leitor de tela: erro em vermelho translúcido, aviso em grafite. */
export function Message({
  text,
  tone = "error",
}: {
  readonly text: string;
  readonly tone?: "error" | "info";
}) {
  const error = tone === "error";
  return (
    <View
      accessibilityLiveRegion="polite"
      style={[styles.box, { backgroundColor: error ? palette.redSoft : palette.surface }]}
    >
      <Icon
        name={error ? "alert" : "check"}
        size={20}
        color={error ? palette.red : palette.lime}
        strokeWidth={2.4}
      />
      <Text
        style={[typography.label, styles.text, { color: error ? palette.red : palette.textSoft }]}
      >
        {text}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  box: {
    flexDirection: "row",
    gap: 10,
    alignItems: "flex-start",
    borderRadius: radius.md,
    padding: spacing.md - 2,
  },
  text: { flex: 1, lineHeight: 20 },
});
