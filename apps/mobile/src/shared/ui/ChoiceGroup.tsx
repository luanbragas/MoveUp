import { StyleSheet, Text, View } from "react-native";
import { Chip } from "./Chip";
import { palette, spacing, typography } from "./theme";

interface Props<T extends string> {
  readonly label: string;
  readonly options: readonly { readonly value: T; readonly label: string }[];
  readonly value: T | null;
  readonly onChange: (value: T) => void;
  readonly error?: string | undefined;
}

/** Escolha única em pílulas (papel, parentesco, objetivo), anunciada como grupo de rádio. */
export function ChoiceGroup<T extends string>({
  label,
  options,
  value,
  onChange,
  error,
}: Props<T>) {
  return (
    <View style={styles.group} accessibilityRole="radiogroup" accessibilityLabel={label}>
      <Text style={[typography.label, { color: palette.textSoft }]}>{label}</Text>
      <View style={styles.options}>
        {options.map((option) => (
          <Chip
            key={option.value}
            role="radio"
            label={option.label}
            selected={option.value === value}
            onPress={() => {
              onChange(option.value);
            }}
          />
        ))}
      </View>
      {error === undefined ? null : (
        <Text accessibilityLiveRegion="polite" style={[typography.label, { color: palette.red }]}>
          {error}
        </Text>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  group: { gap: spacing.sm },
  options: { flexDirection: "row", flexWrap: "wrap", gap: spacing.sm },
});
