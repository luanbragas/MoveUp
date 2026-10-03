import { StyleSheet, View } from "react-native";
import { palette } from "./theme";

interface Props {
  /** Passo atual, começando em 1. */
  readonly current: number;
  readonly total: number;
}

/** Barra de passos de um fluxo curto (cadastro em 3 passos). */
export function Steps({ current, total }: Props) {
  return (
    <View
      accessibilityRole="progressbar"
      accessibilityLabel={`Passo ${String(current)} de ${String(total)}`}
      accessibilityValue={{ min: 1, max: total, now: current }}
      style={styles.row}
    >
      {Array.from({ length: total }, (_, i) => (
        <View
          key={i}
          style={[styles.bar, { backgroundColor: i < current ? palette.lime : palette.line }]}
        />
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: "row", gap: 6, flex: 1 },
  bar: { flex: 1, height: 6, borderRadius: 3 },
});
