import { useRef } from "react";
import { Pressable, StyleSheet, Text, TextInput, View } from "react-native";
import { fonts, palette, radius, spacing, typography } from "./theme";

interface Props {
  readonly label: string;
  readonly value: string;
  readonly onChangeText: (value: string) => void;
  readonly length: number;
  readonly error?: string | undefined;
}

/**
 * Código curto em caixinhas (convite). Um campo de texto invisível recebe a digitação e o colar;
 * as caixinhas só mostram. O leitor de tela lê o campo com o rótulo.
 */
export function CodeInput({ label, value, onChangeText, length, error }: Props) {
  const input = useRef<TextInput>(null);
  const chars = value.slice(0, length).split("");
  const focusIndex = Math.min(chars.length, length - 1);

  return (
    <View style={styles.wrap}>
      <Pressable
        accessible={false}
        onPress={() => {
          input.current?.focus();
        }}
        style={styles.row}
      >
        {Array.from({ length }, (_, i) => (
          <View
            key={i}
            style={[
              styles.box,
              i === Math.floor(length / 2) ? styles.split : null,
              error !== undefined ? styles.error : i === focusIndex ? styles.focus : null,
            ]}
          >
            <Text style={styles.char}>{chars[i] ?? ""}</Text>
          </View>
        ))}
        <TextInput
          ref={input}
          accessibilityLabel={label}
          accessibilityHint={error}
          value={value}
          onChangeText={(text) => {
            onChangeText(
              text
                .toUpperCase()
                .replace(/[^A-Z0-9]/g, "")
                .slice(0, length),
            );
          }}
          autoCapitalize="characters"
          autoComplete="off"
          autoCorrect={false}
          maxLength={length + 4}
          style={styles.hidden}
        />
      </Pressable>
      {error === undefined ? null : (
        <Text accessibilityLiveRegion="polite" style={[typography.small, { color: palette.red }]}>
          {error}
        </Text>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { gap: spacing.sm },
  row: { flexDirection: "row", gap: 6 },
  box: {
    flex: 1,
    height: 60,
    borderRadius: radius.sm,
    backgroundColor: palette.surface,
    alignItems: "center",
    justifyContent: "center",
  },
  split: { marginLeft: spacing.sm },
  focus: { borderWidth: 2, borderColor: palette.lime },
  error: { borderWidth: 2, borderColor: palette.red },
  char: { fontFamily: fonts.number, fontSize: 24, color: palette.text },
  hidden: {
    position: "absolute",
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    opacity: 0,
    color: "transparent",
  },
});
