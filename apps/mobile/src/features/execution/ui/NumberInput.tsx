import { useState } from "react";
import { StyleSheet, Text, TextInput, View } from "react-native";
import { fonts, palette, radius } from "../../../shared/ui/theme";

interface Props {
  readonly label: string;
  readonly value: number | null;
  readonly onChange: (value: number | null) => void;
  readonly decimal?: boolean;
  /** Planejado, embaixo do número (ex.: "8–10 planejado"). */
  readonly hint?: string | null;
  readonly done?: boolean;
}

const show = (value: number | null) => (value === null ? "" : String(value).replace(".", ","));

/** Número grande da tabela de séries: um toque seleciona tudo para trocar rápido. */
export function NumberInput({
  label,
  value,
  onChange,
  decimal = false,
  hint = null,
  done = false,
}: Props) {
  const [text, setText] = useState(show(value));
  const [last, setLast] = useState(value);
  if (value !== last) {
    setLast(value);
    setText(show(value));
  }
  return (
    <View style={styles.wrap}>
      <TextInput
        accessibilityLabel={hint === null ? label : `${label}, ${hint}`}
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
        style={[styles.input, done ? styles.done : null]}
      />
      {hint === null ? null : <Text style={styles.hint}>{hint}</Text>}
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { flex: 1, gap: 2 },
  input: {
    height: 56,
    borderRadius: radius.md,
    backgroundColor: palette.surface,
    color: palette.text,
    fontFamily: fonts.number,
    fontSize: 24,
    textAlign: "center",
  },
  done: { color: palette.lime },
  hint: { fontSize: 11, color: palette.muted, textAlign: "center" },
});
