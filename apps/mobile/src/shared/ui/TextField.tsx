import { useState, type ReactNode } from "react";
import { StyleSheet, Text, TextInput, View, type TextInputProps } from "react-native";
import { palette, radius, spacing, typography } from "./theme";

type Props = {
  readonly label: string;
  readonly error?: string | undefined;
  /** Ação ou ícone à direita do campo (ex.: "Esqueci", ícone de calendário). */
  readonly trailing?: ReactNode;
} & Pick<
  TextInputProps,
  | "value"
  | "onChangeText"
  | "onBlur"
  | "secureTextEntry"
  | "keyboardType"
  | "autoCapitalize"
  | "autoComplete"
  | "textContentType"
  | "placeholder"
  | "multiline"
  | "maxLength"
>;

/** Campo grande com o rótulo dentro; anel lima quando ativo, vermelho com erro (anunciado ao leitor de tela). */
export function TextField({ label, error, trailing, onBlur, ...input }: Props) {
  const [focused, setFocused] = useState(false);
  const ring = error !== undefined ? palette.red : focused ? palette.lime : "transparent";
  return (
    <View style={styles.field}>
      <View style={[styles.box, { borderColor: ring }]}>
        <View style={styles.texts}>
          <Text style={[typography.small, { color: palette.muted }]}>{label}</Text>
          <TextInput
            accessibilityLabel={label}
            accessibilityHint={error}
            placeholderTextColor={palette.muted}
            selectionColor={palette.lime}
            cursorColor={palette.lime}
            style={[
              typography.headline,
              styles.input,
              // texto longo (anamnese, restrição): fonte de corpo e altura mínima de 3 linhas
              input.multiline === true ? styles.multiline : null,
            ]}
            onFocus={() => {
              setFocused(true);
            }}
            onBlur={(event) => {
              setFocused(false);
              onBlur?.(event);
            }}
            {...input}
          />
        </View>
        {trailing}
      </View>
      {error === undefined ? null : (
        <Text accessibilityLiveRegion="polite" style={[typography.label, styles.error]}>
          {error}
        </Text>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  multiline: { ...typography.body, minHeight: 72, textAlignVertical: "top" },
  field: { gap: spacing.xs + 2 },
  box: {
    minHeight: 68,
    borderRadius: radius.md + 2,
    borderWidth: 2,
    backgroundColor: palette.surface,
    paddingHorizontal: spacing.md + 2,
    paddingVertical: 8,
    flexDirection: "row",
    alignItems: "center",
    gap: 10,
  },
  texts: { flex: 1 },
  input: { color: palette.text, fontSize: 18, lineHeight: 24, padding: 0, minHeight: 26 },
  error: { color: palette.red, paddingHorizontal: spacing.xs },
});
