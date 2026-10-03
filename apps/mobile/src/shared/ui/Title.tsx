import { Text } from "react-native";
import { fonts, palette } from "./theme";

interface Props {
  readonly children: string;
  /** Trecho final em destaque (lima ou vermelho), na linha de baixo. */
  readonly accent?: string;
  readonly accentColor?: string;
  readonly size?: number;
  readonly color?: string;
}

/** Título em caixa alta na Archivo larga. Até ~10 letras por linha: quebre com "\n". */
export function Title({
  children,
  accent,
  accentColor = palette.lime,
  size = 40,
  color = palette.text,
}: Props) {
  return (
    <Text
      accessibilityRole="header"
      style={{
        fontFamily: fonts.display,
        fontSize: size,
        lineHeight: Math.round(size * 0.96),
        color,
        textTransform: "uppercase",
      }}
    >
      {children}
      {accent === undefined ? null : <Text style={{ color: accentColor }}>{`\n${accent}`}</Text>}
    </Text>
  );
}
