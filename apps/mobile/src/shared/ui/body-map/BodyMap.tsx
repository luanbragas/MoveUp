import { useId } from "react";
import { View } from "react-native";
import Svg, { Defs, G, Mask, Path, RadialGradient, Rect, Stop } from "react-native-svg";
import { palette } from "../theme";
import { BACK, FRONT } from "./generated/body-paths";
import {
  bestView,
  describeLevels,
  zoomViewBox,
  type BodyPath,
  type BodyView,
  type MuscleLevels,
} from "./muscles";

const DRAWINGS = { front: FRONT, back: BACK } as const;

/** Cores por nível: principal em lima, secundário em lima escuro, resto do corpo em grafite. */
const COLORS = {
  main: palette.lime,
  secondary: "#5E7A1E",
  idle: "#3A3A40",
  base: palette.line,
  baseDark: "#2E2E33",
  outline: palette.background,
} as const;

interface Props {
  readonly levels: MuscleLevels;
  readonly width: number;
  readonly height: number;
  /** Lado do corpo; sem valor, o lado com mais músculo trabalhado. */
  readonly view?: BodyView;
  /** Amplia a região trabalhada (padrão) ou mostra o corpo inteiro. */
  readonly zoom?: boolean;
}

function fillOf(path: BodyPath, levels: MuscleLevels): string {
  if (path.part === null) {
    return "none";
  }
  if (path.part === "base") {
    return COLORS.base;
  }
  if (path.part === "base-dark") {
    return COLORS.baseDark;
  }
  const level = levels[path.part];
  return level === 2 ? COLORS.main : level === 1 ? COLORS.secondary : COLORS.idle;
}

/**
 * Mapa muscular (desenho próprio do MoveUp, assets/body). Por padrão aproxima na região trabalhada
 * e esmaece as bordas, como no "Como fazer" e no resumo do treino.
 */
export function BodyMap({ levels, width, height, view, zoom = true }: Props) {
  const side = view ?? bestView(DRAWINGS, levels);
  const drawing = DRAWINGS[side];
  const box = zoom
    ? zoomViewBox(drawing, levels, width / height)
    : { x: 0, y: 0, width: drawing.width, height: drawing.height };
  const id = useId().replace(/:/g, "");

  return (
    <View
      accessible
      accessibilityRole="image"
      accessibilityLabel={describeLevels(side, levels)}
      style={{ width, height }}
    >
      <Svg
        width={width}
        height={height}
        viewBox={`${String(box.x)} ${String(box.y)} ${String(box.width)} ${String(box.height)}`}
      >
        {zoom ? (
          <Defs>
            <RadialGradient id={`fade-${id}`} cx="50%" cy="45%" rx="60%" ry="55%">
              <Stop offset="0.6" stopColor="#FFFFFF" stopOpacity={1} />
              <Stop offset="1" stopColor="#FFFFFF" stopOpacity={0} />
            </RadialGradient>
            <Mask id={`mask-${id}`}>
              <Rect
                transform={`translate(${String(box.x)} ${String(box.y)})`}
                width={box.width}
                height={box.height}
                fill={`url(#fade-${id})`}
              />
            </Mask>
          </Defs>
        ) : null}
        <G {...(zoom ? { mask: `url(#mask-${id})` } : {})}>
          {drawing.paths.map((path, index) => (
            <Path
              key={index}
              d={path.d}
              fill={fillOf(path, levels)}
              {...(path.evenOdd === true ? { fillRule: "evenodd", clipRule: "evenodd" } : {})}
              {...(path.strokeWidth === undefined
                ? {}
                : {
                    stroke: COLORS.outline,
                    strokeWidth: path.strokeWidth,
                    strokeLinecap: "round",
                    strokeLinejoin: "round",
                  })}
            />
          ))}
        </G>
      </Svg>
    </View>
  );
}
