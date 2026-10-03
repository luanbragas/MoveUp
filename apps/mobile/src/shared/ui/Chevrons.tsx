import Svg, { Path } from "react-native-svg";
import { palette } from "./theme";

interface Props {
  readonly size?: number;
  readonly color?: string;
  /** Quantas setas empilhadas (1 a 3). */
  readonly count?: 1 | 2 | 3;
  /** Opacidade da seta de cima; as de baixo vão esmaecendo. */
  readonly opacity?: number;
}

/** Marca do MoveUp: setas para cima empilhadas. Aparece nos momentos de virada (recorde, convite aceito). */
export function Chevrons({ size = 32, color = palette.lime, count = 3, opacity = 1 }: Props) {
  const stroke = size * 0.16;
  return (
    <Svg
      width={size}
      height={size}
      viewBox={`0 0 ${String(size)} ${String(size)}`}
      accessibilityElementsHidden
      importantForAccessibility="no-hide-descendants"
    >
      {Array.from({ length: count }, (_, i) => (
        <Path
          key={i}
          d={`M${String(size * 0.12)} ${String(size * (0.42 + i * 0.22))} L${String(size / 2)} ${String(size * (0.12 + i * 0.22))} L${String(size * 0.88)} ${String(size * (0.42 + i * 0.22))}`}
          stroke={color}
          strokeWidth={stroke}
          strokeLinecap="round"
          strokeLinejoin="round"
          fill="none"
          opacity={opacity * (1 - i * 0.28)}
        />
      ))}
    </Svg>
  );
}
