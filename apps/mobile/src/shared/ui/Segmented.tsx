import { Pressable, StyleSheet, Text, View } from "react-native";
import { MIN_TOUCH, palette, radius, typography } from "./theme";

interface Props<T extends string> {
  readonly label: string;
  readonly options: readonly { readonly value: T; readonly label: string }[];
  readonly value: T;
  readonly onChange: (value: T) => void;
}

/** Seletor de 2 a 4 opções na mesma linha (ex.: Peso · Medidas · Fotos). */
export function Segmented<T extends string>({ label, options, value, onChange }: Props<T>) {
  return (
    <View accessibilityRole="tablist" accessibilityLabel={label} style={styles.track}>
      {options.map((option) => {
        const selected = option.value === value;
        return (
          <Pressable
            key={option.value}
            accessibilityRole="tab"
            accessibilityLabel={option.label}
            accessibilityState={{ selected }}
            onPress={() => {
              onChange(option.value);
            }}
            style={[styles.segment, selected ? styles.selected : null]}
          >
            <Text style={[typography.label, { color: selected ? palette.onLime : palette.muted }]}>
              {option.label}
            </Text>
          </Pressable>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  track: {
    flexDirection: "row",
    gap: 4,
    padding: 4,
    borderRadius: radius.pill,
    backgroundColor: palette.surface,
  },
  segment: {
    flex: 1,
    minHeight: MIN_TOUCH - 4,
    borderRadius: radius.pill,
    alignItems: "center",
    justifyContent: "center",
  },
  selected: { backgroundColor: palette.lime },
});
