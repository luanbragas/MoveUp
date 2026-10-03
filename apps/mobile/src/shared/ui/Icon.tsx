import Svg, { Circle, Path } from "react-native-svg";
import { palette } from "./theme";

// Ícones de traço único (24×24), desenhados para o MoveUp: um só peso e estilo em todo o app.
type Shape =
  | { readonly d: string; readonly filled?: boolean }
  | { readonly circle: readonly [number, number, number] };

const ICONS = {
  arrow: [{ d: "M5 12h14" }, { d: "M13 6l6 6-6 6" }],
  back: [{ d: "M15 6l-6 6 6 6" }],
  close: [{ d: "M6 6l12 12" }, { d: "M18 6L6 18" }],
  check: [{ d: "M5 12l5 5 9-10" }],
  plus: [{ d: "M12 5v14" }, { d: "M5 12h14" }],
  chevron: [{ d: "M9 6l6 6-6 6" }],
  up: [{ d: "M6 15l6-6 6 6" }],
  down: [{ d: "M6 9l6 6 6-6" }],
  alert: [
    { d: "M12 9v4" },
    { d: "M12 17h.01" },
    { d: "M10.3 3.9 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z" },
  ],
  home: [{ d: "M3 10.5 12 3l9 7.5V21h-6v-6H9v6H3z" }],
  dumbbell: [
    { d: "M6 7v10" },
    { d: "M18 7v10" },
    { d: "M3 10v4" },
    { d: "M21 10v4" },
    { d: "M6 12h12" },
  ],
  trend: [{ d: "M3 17l6-6 4 4 8-8" }, { d: "M15 7h6v6" }],
  chart: [{ d: "M4 20V10" }, { d: "M10 20V4" }, { d: "M16 20v-7" }, { d: "M22 20H2" }],
  bell: [{ d: "M6 8a6 6 0 1 1 12 0c0 7 3 9 3 9H3s3-2 3-9" }, { d: "M10 21a2 2 0 0 0 4 0" }],
  share: [{ d: "M4 12v7h16v-7" }, { d: "M12 3v12" }, { d: "M7 8l5-5 5 5" }],
  chat: [{ d: "M21 12a8 8 0 0 1-11.6 7.1L4 20l1-4.6A8 8 0 1 1 21 12z" }],
  play: [{ d: "M7 4v16l13-8z", filled: true }],
  edit: [{ d: "M4 20h4L19 9l-4-4L4 16z" }, { d: "M13 7l4 4" }],
  logout: [{ d: "M15 4h4v16h-4" }, { d: "M10 8l-4 4 4 4" }, { d: "M6 12h10" }],
  trash: [{ d: "M4 7h16" }, { d: "M9 7V4h6v3" }, { d: "M6 7l1 13h10l1-13" }],
  refresh: [{ d: "M20 11a8 8 0 1 0-2.3 5.7" }, { d: "M20 5v6h-6" }],
  cloudOff: [
    { d: "M3 3l18 18" },
    { d: "M8.5 8.4A6 6 0 0 0 6.4 8 5 5 0 0 0 7 18h10.5" },
    { d: "M18.2 13.6A4.5 4.5 0 0 0 18 9a6 6 0 0 0-7.9-4.8" },
  ],
  shield: [{ d: "M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6z" }],
  doc: [{ d: "M6 3h8l4 4v14H6z" }, { d: "M9 12h6" }, { d: "M9 16h6" }],
  search: [{ circle: [11, 11, 7] }, { d: "M20 20l-3.5-3.5" }],
  copy: [
    { d: "M11 9h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2h-8a2 2 0 0 1-2-2v-8a2 2 0 0 1 2-2z" },
    { d: "M5 15V5a2 2 0 0 1 2-2h10" },
  ],
  user: [{ circle: [12, 8, 4] }, { d: "M4 21c0-4.4 3.6-8 8-8s8 3.6 8 8" }],
  users: [
    { circle: [9, 8, 4] },
    { d: "M2 21c0-3.9 3.1-7 7-7s7 3.1 7 7M16 4a4 4 0 0 1 0 8M22 21c0-3-1.8-5.6-4.5-6.6" },
  ],
  clock: [{ circle: [12, 12, 9] }, { d: "M12 7v5l3 2" }],
  sliders: [
    { d: "M4 6h10M18 6h2M4 12h4M12 12h8M4 18h12" },
    { circle: [16, 6, 2] },
    { circle: [10, 12, 2] },
    { circle: [18, 18, 2] },
  ],
} satisfies Record<string, readonly Shape[]>;

export type IconName = keyof typeof ICONS;

interface Props {
  readonly name: IconName;
  readonly size?: number;
  readonly color?: string;
  readonly strokeWidth?: number;
}

/** Ícone decorativo: quem é lido pelo leitor de tela é o botão ou o texto ao lado. */
export function Icon({ name, size = 22, color = palette.text, strokeWidth = 2 }: Props) {
  const stroke = {
    stroke: color,
    strokeWidth,
    strokeLinecap: "round",
    strokeLinejoin: "round",
  } as const;
  const shapes: readonly Shape[] = ICONS[name];
  return (
    <Svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      accessibilityElementsHidden
      importantForAccessibility="no-hide-descendants"
    >
      {shapes.map((shape, i) => {
        if ("circle" in shape) {
          const [cx, cy, r] = shape.circle;
          return <Circle key={i} cx={cx} cy={cy} r={r} fill="none" {...stroke} />;
        }
        return (
          <Path key={i} d={shape.d} fill={shape.filled === true ? color : "none"} {...stroke} />
        );
      })}
    </Svg>
  );
}
