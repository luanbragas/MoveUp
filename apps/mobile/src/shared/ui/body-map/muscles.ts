// Músculos do desenho do corpo e a escolha do recorte (zoom) — regras puras, testadas.

export type Muscle =
  | "chest"
  | "delts"
  | "traps"
  | "abs"
  | "lats"
  | "biceps"
  | "triceps"
  | "forearms"
  | "quads"
  | "adductors"
  | "abductors"
  | "calves"
  | "lowerback"
  | "glutes"
  | "hamstrings";

/** 2 = principal, 1 = secundário. Músculo ausente = não trabalhado. */
export type MuscleLevels = Readonly<Partial<Record<Muscle, 1 | 2>>>;

export type BodyView = "front" | "back";

export interface BodyPath {
  readonly d: string;
  /** Músculo, corpo sem marcação ("base"/"base-dark") ou só contorno (null). */
  readonly part: Muscle | "base" | "base-dark" | null;
  /** Caixa aproximada [x0, y0, x1, y1] no espaço do desenho. */
  readonly box: readonly [number, number, number, number];
  readonly evenOdd?: boolean;
  readonly strokeWidth?: number;
}

export interface BodyDrawing {
  readonly width: number;
  readonly height: number;
  readonly paths: readonly BodyPath[];
}

export const MUSCLE_NAMES: Readonly<Record<Muscle, string>> = {
  chest: "peitoral",
  delts: "ombros",
  traps: "trapézio",
  abs: "abdômen",
  lats: "dorsal",
  biceps: "bíceps",
  triceps: "tríceps",
  forearms: "antebraço",
  quads: "quadríceps",
  adductors: "adutores",
  abductors: "glúteo médio",
  calves: "panturrilha",
  lowerback: "lombar",
  glutes: "glúteos",
  hamstrings: "posteriores da coxa",
};

export interface ViewBox {
  readonly x: number;
  readonly y: number;
  readonly width: number;
  readonly height: number;
}

function workedBoxes(drawing: BodyDrawing, levels: MuscleLevels) {
  return drawing.paths.flatMap((path) => {
    const level =
      path.part === null || path.part === "base" || path.part === "base-dark"
        ? undefined
        : levels[path.part];
    return level === undefined ? [] : [{ box: path.box, level }];
  });
}

/** Lado do corpo onde há mais área trabalhada (principal pesa o dobro). */
export function bestView(
  drawings: Readonly<Record<BodyView, BodyDrawing>>,
  levels: MuscleLevels,
): BodyView {
  const score = (view: BodyView) =>
    workedBoxes(drawings[view], levels).reduce(
      (sum, { box, level }) => sum + (box[2] - box[0]) * (box[3] - box[1]) * level,
      0,
    );
  return score("back") > score("front") ? "back" : "front";
}

/**
 * Recorte ampliado na região dos músculos trabalhados, com folga, na proporção pedida e sem sair
 * do desenho. Sem músculo trabalhado, o corpo inteiro.
 */
export function zoomViewBox(
  drawing: BodyDrawing,
  levels: MuscleLevels,
  aspect: number,
  pad = 0.14,
): ViewBox {
  const boxes = workedBoxes(drawing, levels).map(({ box }) => box);
  const W = drawing.width;
  const H = drawing.height;
  let [x0, y0, x1, y1] =
    boxes.length === 0
      ? [0, 0, W, H]
      : [
          Math.min(...boxes.map((b) => b[0])),
          Math.min(...boxes.map((b) => b[1])),
          Math.max(...boxes.map((b) => b[2])),
          Math.max(...boxes.map((b) => b[3])),
        ];
  const bw = x1 - x0;
  const bh = y1 - y0;
  x0 -= bw * pad;
  x1 += bw * pad;
  y0 -= bh * pad;
  y1 += bh * pad;
  let cw = x1 - x0;
  let ch = y1 - y0;
  if (cw / ch < aspect) {
    const grow = ch * aspect - cw;
    x0 -= grow / 2;
    cw += grow;
  } else {
    const grow = cw / aspect - ch;
    y0 -= grow / 2;
    ch += grow;
  }
  const x = cw <= W ? Math.max(Math.min(x0, W - cw), 0) : (W - cw) / 2;
  const y = ch <= H ? Math.max(Math.min(y0, H - ch), 0) : (H - ch) / 2;
  return { x: Math.round(x), y: Math.round(y), width: Math.round(cw), height: Math.round(ch) };
}

/** Texto para o leitor de tela: lado e músculos trabalhados. */
export function describeLevels(view: BodyView, levels: MuscleLevels): string {
  const side = view === "front" ? "Frente do corpo" : "Costas";
  const names = (level: 1 | 2) =>
    (Object.keys(levels) as Muscle[])
      .filter((muscle) => levels[muscle] === level)
      .map((muscle) => MUSCLE_NAMES[muscle]);
  const main = names(2);
  const secondary = names(1);
  return [
    side,
    main.length > 0 ? `principal: ${main.join(", ")}` : null,
    secondary.length > 0 ? `secundário: ${secondary.join(", ")}` : null,
  ]
    .filter((part) => part !== null)
    .join("; ");
}
