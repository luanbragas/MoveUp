import { Pressable, StyleSheet, Text, View } from "react-native";
import { MIN_TOUCH, palette, typography } from "./theme";

interface Props {
  /** Texto antes do link, ex.: "Já tem conta?". */
  readonly before?: string;
  readonly label: string;
  readonly onPress: () => void;
}

/** Ação de texto centralizada no rodapé ("Já tem conta? Entrar"). Área de toque de 48. */
export function TextLink({ before, label, onPress }: Props) {
  return (
    <View style={styles.row}>
      {before === undefined ? null : (
        <Text style={[typography.small, { color: palette.muted, fontSize: 15 }]}>{before}</Text>
      )}
      <Pressable
        accessibilityRole="button"
        accessibilityLabel={before === undefined ? label : `${before} ${label}`}
        onPress={onPress}
        style={styles.link}
      >
        <Text style={[typography.label, { color: palette.text, fontSize: 15 }]}>{label}</Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: "row", alignItems: "center", justifyContent: "center", gap: 2 },
  link: { minHeight: MIN_TOUCH, justifyContent: "center", paddingHorizontal: 6 },
});
