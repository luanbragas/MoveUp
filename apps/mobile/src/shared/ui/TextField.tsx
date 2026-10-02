import { StyleSheet, Text, TextInput, View, type TextInputProps } from "react-native";
import { MIN_TOUCH, spacing, typography, useColors } from "./theme";

type Props = {
  readonly label: string;
  readonly error?: string | undefined;
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
>;

/** Campo com rótulo visível e erro anunciado ao leitor de tela. */
export function TextField({ label, error, ...input }: Props) {
  const colors = useColors();
  return (
    <View style={styles.field}>
      <Text style={[typography.label, { color: colors.text }]}>{label}</Text>
      <TextInput
        accessibilityLabel={label}
        accessibilityHint={error}
        placeholderTextColor={colors.textMuted}
        style={[
          typography.body,
          styles.input,
          {
            color: colors.text,
            backgroundColor: colors.surface,
            borderColor: error === undefined ? colors.border : colors.danger,
          },
        ]}
        {...input}
      />
      {error === undefined ? null : (
        <Text accessibilityLiveRegion="polite" style={[typography.small, { color: colors.danger }]}>
          {error}
        </Text>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  field: { gap: spacing.xs },
  input: { minHeight: MIN_TOUCH, borderWidth: 1, borderRadius: 10, paddingHorizontal: spacing.md },
});
