import { useState } from "react";
import { StyleSheet, Text, TextInput, View } from "react-native";
import { fonts, palette, radius, typography } from "../../../shared/ui/theme";

interface Props {
  readonly label: string;
  readonly value: number | null;
  readonly onChange: (value: number | null) => void;
  /** Aceita vírgula ou ponto (carga 62,5). */
  readonly decimal?: boolean;
  readonly compact?: boolean;
}

function show(value: number | null): string {
  return value === null ? "" : String(value).replace(".", ",");
}

/** Campo numérico curto (séries, tempos). Vazio = sem valor. */
export function NumberField({ label, value, onChange, decimal = false, compact = false }: Props) {
  const [text, setText] = useState(show(value));
  const [last, setLast] = useState(value);
  if (value !== last) {
    // valor mudou de fora (ex.: "aplicar a todas"): mostra o novo
    setLast(value);
    setText(show(value));
  }

  return (
    <View style={compact ? styles.compact : styles.wrap}>
      {compact ? null : <Text style={[typography.small, { color: palette.muted }]}>{label}</Text>}
      <TextInput
        accessibilityLabel={label}
        value={text}
        onChangeText={(typed) => {
          const clean = typed.replace(decimal ? /[^0-9.,]/g : /[^0-9]/g, "");
          setText(clean);
          const parsed = clean === "" ? null : Number(clean.replace(",", "."));
          const next = parsed === null || Number.isNaN(parsed) ? null : parsed;
          setLast(next);
          onChange(next);
        }}
        keyboardType={decimal ? "decimal-pad" : "number-pad"}
        selectTextOnFocus
        style={[styles.input, compact ? styles.inputCompact : null]}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { flex: 1, gap: 4 },
  compact: { flex: 1 },
  input: {
    minHeight: 48,
    borderRadius: radius.sm,
    backgroundColor: palette.surface2,
    color: palette.text,
    fontFamily: fonts.number,
    fontSize: 18,
    textAlign: "center",
    paddingHorizontal: 6,
  },
  inputCompact: { minHeight: 48, fontSize: 17 },
});
