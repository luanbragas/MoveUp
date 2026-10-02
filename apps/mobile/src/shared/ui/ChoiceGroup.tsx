import { Pressable, StyleSheet, Text, View } from "react-native";
import { MIN_TOUCH, spacing, typography, useColors } from "./theme";

interface Props<T extends string> {
  readonly label: string;
  readonly options: readonly { readonly value: T; readonly label: string }[];
  readonly value: T | null;
  readonly onChange: (value: T) => void;
  readonly error?: string | undefined;
}

/** Escolha única (papel, parentesco), anunciada como grupo de rádio. */
export function ChoiceGroup<T extends string>({
  label,
  options,
  value,
  onChange,
  error,
}: Props<T>) {
  const colors = useColors();
  return (
    <View style={styles.group} accessibilityRole="radiogroup" accessibilityLabel={label}>
      <Text style={[typography.label, { color: colors.text }]}>{label}</Text>
      <View style={styles.options}>
        {options.map((option) => {
          const selected = option.value === value;
          return (
            <Pressable
              key={option.value}
              accessibilityRole="radio"
              accessibilityState={{ selected }}
              accessibilityLabel={option.label}
              onPress={() => {
                onChange(option.value);
              }}
              style={[
                styles.option,
                {
                  borderColor: selected ? colors.primary : colors.border,
                  backgroundColor: selected ? colors.surface : colors.background,
                },
              ]}
            >
              <Text style={[typography.body, { color: colors.text }]}>{option.label}</Text>
            </Pressable>
          );
        })}
      </View>
      {error === undefined ? null : (
        <Text accessibilityLiveRegion="polite" style={[typography.small, { color: colors.danger }]}>
          {error}
        </Text>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  group: { gap: spacing.xs },
  options: { flexDirection: "row", flexWrap: "wrap", gap: spacing.sm },
  option: {
    minHeight: MIN_TOUCH,
    borderWidth: 2,
    borderRadius: 10,
    paddingHorizontal: spacing.md,
    justifyContent: "center",
  },
});
