// Treino planejado no app: rascunho do editor e as operações puras sobre ele.

export type BlockMethod =
  "sequential" | "superset" | "circuit" | "hiit" | "emom" | "amrap" | "intervals";
export type SetType = "warmup" | "normal" | "drop" | "rest_pause" | "failure";
export type TrackingType = "reps_load" | "reps_only" | "time" | "distance_time";

export interface SetDraft {
  readonly type: SetType;
  readonly repsMin: number | null;
  readonly repsMax: number | null;
  readonly loadKg: number | null;
  readonly durationSeconds: number | null;
  readonly distanceM: number | null;
  readonly targetRir: number | null;
  readonly restSeconds: number | null;
}

export interface ExerciseDraft {
  /** Chave local (lista e edição); não vai para a API. */
  readonly key: string;
  readonly exerciseId: string;
  readonly exerciseName: string;
  readonly trackingType: TrackingType;
  readonly primaryMuscle: string | null;
  readonly restSeconds: number | null;
  readonly notes: string | null;
  readonly sets: readonly SetDraft[];
}

export interface BlockDraft {
  readonly key: string;
  readonly name: string | null;
  readonly method: BlockMethod;
  readonly preset: "tabata" | null;
  readonly rounds: number | null;
  readonly workSeconds: number | null;
  readonly restSeconds: number | null;
  readonly restBetweenRounds: number | null;
  readonly durationSeconds: number | null;
  readonly exercises: readonly ExerciseDraft[];
}

export interface WorkoutDraft {
  readonly name: string;
  readonly goal: string | null;
  readonly estimatedMinutes: number | null;
  readonly notes: string | null;
  readonly blocks: readonly BlockDraft[];
}

/** Treino como veio da API: o rascunho mais o que identifica a edição. */
export interface Workout {
  readonly id: string;
  readonly template: boolean;
  readonly programId: string | null;
  /** Vai no If-Match ao salvar. */
  readonly revision: number;
  /** Versão atual do conteúdo: vai na sessão do treino presencial. */
  readonly versionId: string;
  readonly versionNumber: number;
  readonly draft: WorkoutDraft;
}

/** Exercício escolhido na biblioteca (o mínimo que o editor precisa). */
export interface PickedExercise {
  readonly id: string;
  readonly name: string;
  readonly trackingType: TrackingType;
  readonly primaryMuscle: string | null;
}

let sequence = 0;
/** Chave local única para listas (não precisa ser aleatória). */
export function newKey(prefix: string): string {
  sequence += 1;
  return `${prefix}-${String(sequence)}`;
}

/** Séries iniciais conforme o que o aluno registra. */
export function defaultSets(tracking: TrackingType): SetDraft[] {
  const base: SetDraft = {
    type: "normal",
    repsMin: null,
    repsMax: null,
    loadKg: null,
    durationSeconds: null,
    distanceM: null,
    targetRir: null,
    restSeconds: null,
  };
  switch (tracking) {
    case "reps_load":
    case "reps_only":
      return [1, 2, 3].map(() => ({ ...base, repsMin: 8, repsMax: 12, restSeconds: 60 }));
    case "time":
      return [1, 2, 3].map(() => ({ ...base, durationSeconds: 30, restSeconds: 30 }));
    case "distance_time":
      return [{ ...base, durationSeconds: 600 }];
  }
}

export function newBlock(method: BlockMethod): BlockDraft {
  const timing: Pick<
    BlockDraft,
    "preset" | "rounds" | "workSeconds" | "restSeconds" | "restBetweenRounds" | "durationSeconds"
  > =
    method === "hiit"
      ? {
          preset: "tabata",
          rounds: 8,
          workSeconds: 20,
          restSeconds: 10,
          restBetweenRounds: null,
          durationSeconds: null,
        }
      : method === "intervals"
        ? {
            preset: null,
            rounds: 6,
            workSeconds: 40,
            restSeconds: 20,
            restBetweenRounds: null,
            durationSeconds: null,
          }
        : method === "circuit"
          ? {
              preset: null,
              rounds: 3,
              workSeconds: null,
              restSeconds: null,
              restBetweenRounds: 60,
              durationSeconds: null,
            }
          : method === "emom" || method === "amrap"
            ? {
                preset: null,
                rounds: null,
                workSeconds: null,
                restSeconds: null,
                restBetweenRounds: null,
                durationSeconds: 600,
              }
            : {
                preset: null,
                rounds: null,
                workSeconds: null,
                restSeconds: null,
                restBetweenRounds: null,
                durationSeconds: null,
              };
  return { key: newKey("block"), name: null, method, exercises: [], ...timing };
}

/** Blocos por tempo: o relógio manda, as séries são opcionais. */
export function isTimed(method: BlockMethod): boolean {
  return method === "hiit" || method === "intervals" || method === "emom" || method === "amrap";
}

function mapBlock(
  draft: WorkoutDraft,
  blockKey: string,
  change: (block: BlockDraft) => BlockDraft,
): WorkoutDraft {
  return { ...draft, blocks: draft.blocks.map((b) => (b.key === blockKey ? change(b) : b)) };
}

function move<T>(items: readonly T[], index: number, delta: -1 | 1): readonly T[] {
  const target = index + delta;
  if (index < 0 || target < 0 || target >= items.length) {
    return items;
  }
  const next = [...items];
  const [item] = next.splice(index, 1);
  if (item !== undefined) {
    next.splice(target, 0, item);
  }
  return next;
}

export const edit = {
  addBlock(draft: WorkoutDraft, method: BlockMethod): WorkoutDraft {
    return { ...draft, blocks: [...draft.blocks, newBlock(method)] };
  },
  removeBlock(draft: WorkoutDraft, blockKey: string): WorkoutDraft {
    return { ...draft, blocks: draft.blocks.filter((b) => b.key !== blockKey) };
  },
  moveBlock(draft: WorkoutDraft, blockKey: string, delta: -1 | 1): WorkoutDraft {
    return {
      ...draft,
      blocks: move(
        draft.blocks,
        draft.blocks.findIndex((b) => b.key === blockKey),
        delta,
      ),
    };
  },
  updateBlock(
    draft: WorkoutDraft,
    blockKey: string,
    patch: Partial<Omit<BlockDraft, "key" | "exercises">>,
  ): WorkoutDraft {
    return mapBlock(draft, blockKey, (b) => ({ ...b, ...patch }));
  },
  addExercises(
    draft: WorkoutDraft,
    blockKey: string,
    picked: readonly PickedExercise[],
  ): WorkoutDraft {
    return mapBlock(draft, blockKey, (b) => ({
      ...b,
      exercises: [
        ...b.exercises,
        ...picked.map((p) => ({
          key: newKey("exercise"),
          exerciseId: p.id,
          exerciseName: p.name,
          trackingType: p.trackingType,
          primaryMuscle: p.primaryMuscle,
          restSeconds: null,
          notes: null,
          sets: isTimed(b.method) ? [] : defaultSets(p.trackingType),
        })),
      ],
    }));
  },
  removeExercise(draft: WorkoutDraft, blockKey: string, exerciseKey: string): WorkoutDraft {
    return mapBlock(draft, blockKey, (b) => ({
      ...b,
      exercises: b.exercises.filter((e) => e.key !== exerciseKey),
    }));
  },
  moveExercise(
    draft: WorkoutDraft,
    blockKey: string,
    exerciseKey: string,
    delta: -1 | 1,
  ): WorkoutDraft {
    return mapBlock(draft, blockKey, (b) => ({
      ...b,
      exercises: move(
        b.exercises,
        b.exercises.findIndex((e) => e.key === exerciseKey),
        delta,
      ),
    }));
  },
  updateExercise(
    draft: WorkoutDraft,
    blockKey: string,
    exerciseKey: string,
    patch: Partial<Pick<ExerciseDraft, "sets" | "notes" | "restSeconds">>,
  ): WorkoutDraft {
    return mapBlock(draft, blockKey, (b) => ({
      ...b,
      exercises: b.exercises.map((e) => (e.key === exerciseKey ? { ...e, ...patch } : e)),
    }));
  },
};

/** Problema que impede salvar, já com a mensagem do usuário. */
export interface DraftProblem {
  readonly blockKey: string | null;
  readonly code:
    | "name"
    | "block-empty"
    | "superset-needs-two"
    | "circuit-needs-two"
    | "sets-required"
    | "reps-invalid";
}

/** Mesmas regras do backend, para avisar antes de salvar (o backend continua sendo a regra final). */
export function problemsOf(draft: WorkoutDraft): readonly DraftProblem[] {
  const problems: DraftProblem[] = [];
  if (draft.name.trim() === "") {
    problems.push({ blockKey: null, code: "name" });
  }
  for (const b of draft.blocks) {
    if (b.exercises.length === 0) {
      problems.push({ blockKey: b.key, code: "block-empty" });
      continue;
    }
    if (b.method === "superset" && b.exercises.length < 2) {
      problems.push({ blockKey: b.key, code: "superset-needs-two" });
    }
    if (b.method === "circuit" && b.exercises.length < 2) {
      problems.push({ blockKey: b.key, code: "circuit-needs-two" });
    }
    if (
      (b.method === "sequential" || b.method === "superset") &&
      b.exercises.some((e) => e.sets.length === 0)
    ) {
      problems.push({ blockKey: b.key, code: "sets-required" });
    }
    const badReps = b.exercises.some((e) =>
      e.sets.some((s) => s.repsMin !== null && s.repsMax !== null && s.repsMin > s.repsMax),
    );
    if (badReps) {
      problems.push({ blockKey: b.key, code: "reps-invalid" });
    }
  }
  return problems;
}

const fmt = (n: number) => String(n).replace(".", ",");

function repsLabel(s: SetDraft): string | null {
  if (s.repsMin === null && s.repsMax === null) {
    return null;
  }
  if (s.repsMin !== null && s.repsMax !== null && s.repsMin !== s.repsMax) {
    return `${String(s.repsMin)} a ${String(s.repsMax)} reps`;
  }
  return `${String(s.repsMin ?? s.repsMax)} reps`;
}

function timeLabel(seconds: number): string {
  return seconds >= 60 && seconds % 60 === 0
    ? `${String(seconds / 60)} min`
    : `${String(seconds)} s`;
}

/**
 * Resumo do exercício com unidades: "3 séries · 8 a 12 reps · 55 kg · desc. 90 s". Quando as
 * séries diferem (pirâmide), mostra a faixa de carga.
 */
export function summarize(exercise: ExerciseDraft): string {
  const sets = exercise.sets.filter((s) => s.type !== "warmup");
  const [first] = sets;
  if (first === undefined) {
    return exercise.sets.length === 0 ? "pelo tempo do bloco" : "só aquecimento";
  }
  const parts = [sets.length === 1 ? "1 série" : `${String(sets.length)} séries`];
  const reps = repsLabel(first);
  if (reps !== null) {
    parts.push(reps);
  }
  const loads = sets.map((s) => s.loadKg).filter((kg): kg is number => kg !== null);
  if (loads.length > 0) {
    const min = Math.min(...loads);
    const max = Math.max(...loads);
    parts.push(min === max ? `${fmt(min)} kg` : `${fmt(min)} a ${fmt(max)} kg`);
  }
  if (first.durationSeconds !== null) {
    parts.push(timeLabel(first.durationSeconds));
  }
  if (first.distanceM !== null) {
    parts.push(
      first.distanceM >= 1000
        ? `${fmt(first.distanceM / 1000)} km`
        : `${String(first.distanceM)} m`,
    );
  }
  const rest = first.restSeconds ?? exercise.restSeconds;
  if (rest !== null) {
    parts.push(`desc. ${timeLabel(rest)}`);
  }
  return parts.join(" · ");
}

/** Resumo do bloco por tempo: "Tabata · 8 × 20 s / 10 s", "EMOM · 10 min". */
export function blockTiming(block: BlockDraft): string | null {
  switch (block.method) {
    case "hiit":
    case "intervals":
      return block.rounds !== null && block.workSeconds !== null && block.restSeconds !== null
        ? `${String(block.rounds)} × ${String(block.workSeconds)} s / ${String(block.restSeconds)} s`
        : null;
    case "emom":
    case "amrap":
      return block.durationSeconds === null ? null : timeLabel(block.durationSeconds);
    case "circuit":
      return block.rounds === null ? null : `${String(block.rounds)} rodadas`;
    case "sequential":
    case "superset":
      return null;
  }
}

/** Totais do rodapé do editor: exercícios e séries de trabalho. */
export function totals(draft: WorkoutDraft): { exercises: number; sets: number } {
  let exercises = 0;
  let sets = 0;
  for (const b of draft.blocks) {
    exercises += b.exercises.length;
    for (const e of b.exercises) {
      sets += e.sets.filter((s) => s.type !== "warmup").length;
    }
  }
  return { exercises, sets };
}
