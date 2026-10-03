import { letterOf, weekdaysLabel } from "./program";
import {
  blockTiming,
  defaultSets,
  edit,
  problemsOf,
  summarize,
  totals,
  type ExerciseDraft,
  type PickedExercise,
  type SetDraft,
  type WorkoutDraft,
} from "./workout";

const bench: PickedExercise = {
  id: "bench",
  name: "Supino reto com barra",
  trackingType: "reps_load",
  primaryMuscle: "chest",
};
const plank: PickedExercise = {
  id: "plank",
  name: "Prancha",
  trackingType: "time",
  primaryMuscle: "abs",
};

const empty: WorkoutDraft = {
  name: "Treino A",
  goal: null,
  estimatedMinutes: null,
  notes: null,
  blocks: [],
};

function firstBlock(draft: WorkoutDraft) {
  const block = draft.blocks[0];
  if (block === undefined) {
    throw new Error("sem bloco");
  }
  return block;
}

describe("editor de treino", () => {
  it("bloco novo com exercícios ganha séries padrão pelo tipo de registro", () => {
    let draft = edit.addBlock(empty, "sequential");
    const key = firstBlock(draft).key;
    draft = edit.addExercises(draft, key, [bench, plank]);

    const [b, p] = firstBlock(draft).exercises;
    expect(b?.sets).toHaveLength(3);
    expect(b?.sets[0]).toMatchObject({ repsMin: 8, repsMax: 12, restSeconds: 60 });
    expect(p?.sets[0]).toMatchObject({ durationSeconds: 30 });
    expect(totals(draft)).toEqual({ exercises: 2, sets: 6 });
  });

  it("em bloco por tempo o exercício entra sem séries; Tabata já vem 8 × 20/10", () => {
    let draft = edit.addBlock(empty, "hiit");
    const block = firstBlock(draft);
    draft = edit.addExercises(draft, block.key, [bench]);

    expect(firstBlock(draft).exercises[0]?.sets).toEqual([]);
    expect(block.preset).toBe("tabata");
    expect(blockTiming(block)).toBe("8 × 20 s / 10 s");
    expect(blockTiming(edit.addBlock(empty, "emom").blocks[0] ?? block)).toBe("10 min");
  });

  it("reordena e remove sem sair dos limites", () => {
    let draft = edit.addBlock(edit.addBlock(empty, "sequential"), "superset");
    const [a, b] = draft.blocks;
    draft = edit.moveBlock(draft, b?.key ?? "", -1);
    expect(draft.blocks.map((x) => x.method)).toEqual(["superset", "sequential"]);
    expect(edit.moveBlock(draft, a?.key ?? "", 1).blocks.map((x) => x.method)).toEqual([
      "superset",
      "sequential",
    ]);
    expect(edit.removeBlock(draft, a?.key ?? "").blocks).toHaveLength(1);
  });

  it("aponta o que impede salvar, como o backend", () => {
    let draft = edit.addBlock({ ...empty, name: " " }, "superset");
    const key = firstBlock(draft).key;
    expect(problemsOf(draft).map((p) => p.code)).toEqual(["name", "block-empty"]);

    draft = edit.addExercises({ ...draft, name: "A" }, key, [bench]);
    expect(problemsOf(draft).map((p) => p.code)).toEqual(["superset-needs-two"]);
  });

  it("resumo com unidades e faixa de carga na pirâmide", () => {
    const base = { ...defaultSets("reps_load")[0], restSeconds: 90 } as SetDraft;
    const sets: SetDraft[] = [0, 1, 2].map((i) => ({ ...base, loadKg: 50 + i * 5 }));
    const exercise: ExerciseDraft = {
      key: "e",
      exerciseId: "bench",
      exerciseName: "Supino",
      trackingType: "reps_load",
      primaryMuscle: "chest",
      restSeconds: null,
      notes: null,
      sets: [{ ...base, type: "warmup", loadKg: 20 }, ...sets],
    };

    expect(summarize(exercise)).toBe("3 séries · 8 a 12 reps · 50 a 60 kg · desc. 90 s");
    expect(summarize({ ...exercise, sets: [{ ...base, loadKg: 62.5 }] })).toBe(
      "1 série · 8 a 12 reps · 62,5 kg · desc. 90 s",
    );
  });

  it("letras e dias da agenda", () => {
    expect(letterOf(1)).toBe("A");
    expect(letterOf(3)).toBe("C");
    expect(weekdaysLabel([4, 1])).toBe("seg e qui");
    expect(weekdaysLabel([1, 3, 5])).toBe("seg, qua e sex");
  });
});
