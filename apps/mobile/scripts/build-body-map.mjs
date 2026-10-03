// Gera src/shared/ui/body-map/generated/body-paths.ts a partir do desenho do corpo
// (assets/body/front.svg e back.svg). Cada grupo muscular foi pintado com uma cor própria no
// desenho; aqui a cor vira o nome do músculo. Rode depois de mudar o desenho:
//   pnpm --filter mobile body-map:generate
import { readFile, writeFile, mkdir } from "node:fs/promises";
import { dirname, join } from "node:path";
import process from "node:process";
import { fileURLToPath } from "node:url";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const out = join(root, "src/shared/ui/body-map/generated/body-paths.ts");

// cor do desenho → músculo; "base" = corpo sem músculo marcado (pele/contorno)
const BASE = { "#504D4D": "base", "#676363": "base-dark" };
const FRONT = {
  "#83B315": "chest",
  "#EEEE33": "delts",
  "#D98F04": "traps",
  "#08CA15": "abs",
  "#AD2D2D": "lats",
  "#D85A5A": "biceps",
  "#FF0C82": "triceps",
  "#D78B5C": "forearms",
  "#2FCACA": "quads",
  "#411A9E": "adductors",
  "#8810BF": "abductors",
  "#0F4CA8": "calves",
};
const BACK = {
  "#8C5F87": "traps",
  "#D98F04": "traps",
  "#EEEE33": "delts",
  "#AD2D2D": "lats",
  "#83B315": "lowerback",
  "#08CA15": "abs",
  "#FF0C82": "triceps",
  "#D78B5C": "forearms",
  black: "glutes",
  "#8810BF": "abductors",
  "#D85A5A": "hamstrings",
  "#2FCACA": "quads",
  "#411A9E": "adductors",
  "#0F4CA8": "calves",
};

const attr = (tag, name) => tag.match(new RegExp(`\\s${name}="([^"]*)"`))?.[1];

/** Caixa aproximada de um path absoluto (exportado do Figma: M/L/C/Q/H/V/Z). */
function bbox(d) {
  const xs = [];
  const ys = [];
  let cmd = null;
  let nums = [];
  const flush = () => {
    if (cmd === "H") xs.push(...nums);
    else if (cmd === "V") ys.push(...nums);
    else if (cmd !== null && cmd !== "Z") {
      nums.forEach((n, i) => (i % 2 === 0 ? xs : ys).push(n));
    }
  };
  for (const token of d.match(/[A-Za-z]|-?\d+(?:\.\d+)?/g) ?? []) {
    if (/[A-Za-z]/.test(token)) {
      flush();
      cmd = token.toUpperCase();
      nums = [];
    } else {
      nums.push(Number(token));
    }
  }
  flush();
  const r = (n) => Math.round(n);
  return [r(Math.min(...xs)), r(Math.min(...ys)), r(Math.max(...xs)), r(Math.max(...ys))];
}

async function convert(file, mapping) {
  const svg = await readFile(join(root, "assets/body", file), "utf8");
  const [, , width, height] = svg
    .match(/viewBox="([^"]+)"/)[1]
    .split(/\s+/)
    .map(Number);
  const paths = [];
  for (const [tag] of svg.matchAll(/<path\b[^>]*>/g)) {
    const raw = attr(tag, "d");
    if (raw === undefined) continue;
    const d = raw.replace(/(\d+\.\d)\d+/g, "$1"); // 1 casa decimal basta no tamanho da tela
    const fill = attr(tag, "fill");
    const part = fill === undefined || fill === "none" ? null : (BASE[fill] ?? mapping[fill]);
    if (fill !== undefined && fill !== "none" && part === undefined) {
      throw new Error(`${file}: cor sem músculo no mapa: ${fill}`);
    }
    const entry = { d, part, box: bbox(d) };
    if (attr(tag, "fill-rule") === "evenodd") entry.evenOdd = true;
    const stroke = attr(tag, "stroke");
    if (stroke !== undefined) entry.strokeWidth = Number(attr(tag, "stroke-width") ?? 1);
    paths.push(entry);
  }
  return { width, height, paths };
}

const front = await convert("front.svg", FRONT);
const back = await convert("back.svg", BACK);
await mkdir(dirname(out), { recursive: true });
await writeFile(
  out,
  `// GERADO por scripts/build-body-map.mjs a partir de assets/body/*.svg. Não editar à mão.\n` +
    `import type { BodyDrawing } from "../muscles";\n\n` +
    `export const FRONT: BodyDrawing = ${JSON.stringify(front)};\n\n` +
    `export const BACK: BodyDrawing = ${JSON.stringify(back)};\n`,
);
process.stdout.write(`body-paths.ts: ${front.paths.length} + ${back.paths.length} traços\n`);
