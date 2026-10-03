import { BACK, FRONT } from "./generated/body-paths";
import { bestView, describeLevels, zoomViewBox, type BodyDrawing } from "./muscles";

const DRAWINGS = { front: FRONT, back: BACK };

describe("mapa muscular", () => {
  it("o desenho tem todos os músculos que o app usa", () => {
    const parts = (drawing: BodyDrawing) => new Set(drawing.paths.map((path) => path.part));
    for (const muscle of ["chest", "delts", "abs", "biceps", "triceps", "quads", "calves"]) {
      expect(parts(FRONT)).toContain(muscle);
    }
    for (const muscle of ["lats", "lowerback", "glutes", "hamstrings", "traps"]) {
      expect(parts(BACK)).toContain(muscle);
    }
  });

  it("escolhe o lado com mais músculo trabalhado", () => {
    expect(bestView(DRAWINGS, { chest: 2, triceps: 1 })).toBe("front");
    expect(bestView(DRAWINGS, { lats: 2, hamstrings: 2, glutes: 1 })).toBe("back");
    expect(bestView(DRAWINGS, {})).toBe("front");
  });

  it("supino aproxima no tronco; sem músculo, mostra o corpo inteiro", () => {
    const chest = zoomViewBox(FRONT, { chest: 2, triceps: 2, delts: 1 }, 1);
    // tronco: bem menor que o desenho e na metade de cima
    expect(chest.height).toBeLessThan(FRONT.height * 0.5);
    expect(chest.y + chest.height).toBeLessThan(FRONT.height * 0.75);
    expect(chest.width / chest.height).toBeCloseTo(1, 1);

    const whole = zoomViewBox(FRONT, {}, FRONT.width / FRONT.height, 0);
    expect(whole).toEqual({ x: 0, y: 0, width: FRONT.width, height: FRONT.height });
  });

  it("o recorte nunca sai do desenho", () => {
    const calves = zoomViewBox(FRONT, { calves: 2 }, 1);
    expect(calves.x).toBeGreaterThanOrEqual(0);
    expect(calves.y).toBeGreaterThanOrEqual(0);
    expect(calves.y + calves.height).toBeLessThanOrEqual(FRONT.height);
  });

  it("descreve para o leitor de tela", () => {
    expect(describeLevels("front", { chest: 2, delts: 1 })).toBe(
      "Frente do corpo; principal: peitoral; secundário: ombros",
    );
  });
});
