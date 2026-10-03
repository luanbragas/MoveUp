import { Pressable, StyleSheet } from "react-native";
import { Icon, type IconName } from "./Icon";
import { MIN_TOUCH, palette } from "./theme";

interface Props {
  readonly icon: IconName;
  /** Lido pelo leitor de tela (o botão só tem ícone). */
  readonly label: string;
  readonly onPress: () => void;
}

/** Botão redondo só com ícone: voltar, fechar, ação no cabeçalho. */
export function RoundButton({ icon, label, onPress }: Props) {
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={label}
      onPress={onPress}
      style={({ pressed }) => [styles.button, pressed ? styles.pressed : null]}
    >
      <Icon name={icon} size={20} color={palette.text} strokeWidth={2.4} />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  button: {
    width: MIN_TOUCH,
    height: MIN_TOUCH,
    borderRadius: MIN_TOUCH / 2,
    backgroundColor: palette.surface2,
    alignItems: "center",
    justifyContent: "center",
  },
  pressed: { opacity: 0.7 },
});
