// Treino em execução e realizado, no aparelho (offline-first). Operações puras: a tela chama, o
// SQLite guarda, o sync envia a sessão inteira.

export type SessionStatus = "in_progress" | "completed" | "partial" | "abandoned";
export type ExerciseStatus = "done" | "skipped" | "substituted";
export type SyncStatus = "pending" | "synced";

/** O planejado da série, para pré-preencher e comparar. */
export interface PlannedSetValues {
  readonly type: string;
  readonly repsMin: number | null;
  readonly repsMax: number | null;
  readonly loadKg: number | null;
  readonly durationSeconds: number | null;
  readonly distanceM: number | null;
  readonly restSeconds: number | null;
}

export interface SessionSet {
  readonly id: string;
  readonly setNumber: number;
  readonly type: string;
  readonly planned: PlannedSetValues | null;
  readonly reps: number | null;
  readonly loadKg: number | null;
  readonly durationSeconds: number | null;
  readonly distanceM: number | null;
  readonly rir: number | null;
  readonly completed: boolean;
  readonly completedAt: string | null;
}

export interface SessionExercise {
  readonly id: string;
  readonly exerciseId: string;
  readonly name: string;
  readonly trackingType: string;
  readonly primaryMuscle: string | null;
  readonly secondaryMuscles: readonly string[];
  readonly blockIndex: number;
  readonly method: string;
  readonly position: number;
  readonly status: ExerciseStatus;
  readonly substitutedFrom: string | null;
  readonly restSeconds: number | null;
  readonly notes: string | null;
  readonly sets: readonly SessionSet[];
}

export interface BlockTiming {
  readonly method: string;
  readonly preset: string | null;
  readonly rounds: number | null;
  readonly workSeconds: number | null;
  readonly restSeconds: number | null;
  readonly restBetweenRounds: number | null;
  readonly durationSeconds: number | null;
  readonly name: string | null;
}

export interface PainEntry {
  readonly id: string;
  readonly bodyRegion: string;
  readonly exerciseId: string | null;
  readonly intensity: number | null;
}

export interface Feedback {
  readonly effort: number;
  readonly comment: string | null;
  readonly pains: readonly PainEntry[];
}

export interface Session {
  readonly id: string;
  readonly linkId: string;
  readonly programId: string | null;
  readonly workoutId: string | null;
  readonly workoutVersionId: string | null;
  readonly workoutName: string;
  readonly performedBy: "client" | "professional";
  readonly status: SessionStatus;
  readonly startedAt: string;
  readonly finishedAt: string | null;
  readonly clientUpdatedAt: string;
  readonly blocks: readonly BlockTiming[];
  readonly exercises: readonly SessionExercise[];
  readonly feedback: Feedback | null;
  /** Edição depois de enviada volta a pendente. */
  readonly syncStatus: SyncStatus;
}

/** O planejado que inicia a sessão (vem do sync do aluno ou do GET do treino no presencial). */
export interface PlannedInput {
  readonly linkId: string;
  readonly programId: string | null;
  readonly workoutId: string;
  readonly versionId: string;
  readonly name: string;
  readonly blocks: readonly (BlockTiming & {
    readonly exercises: readonly {
      readonly exerciseId: string;
      readonly name: string;
      readonly trackingType: string;
      readonly primaryMuscle: string | null;
      readonly secondaryMuscles: readonly string[];
      readonly restSeconds: number | null;
      readonly notes: string | null;
      readonly sets: readonly PlannedSetValues[];
    }[];
  })[];
}

export type NewId = () => string;

const iso = (now: Date) => now.toISOString();

/** Inicia a sessão local com as séries pré-preenchidas com o planejado. */
export function startSession(
  planned: PlannedInput,
  performedBy: Session["performedBy"],
  now: Date,
  newId: NewId,
): Session {
  let position = 0;
  const exercises: SessionExercise[] = [];
  planned.blocks.forEach((block, blockIndex) => {
    for (const e of block.exercises) {
      position += 1;
      const plannedSets =
        e.sets.length > 0
          ? e.sets
          : [
              {
                type: "normal",
                repsMin: null,
                repsMax: null,
                loadKg: null,
                durationSeconds: null,
                distanceM: null,
                restSeconds: null,
              },
            ];
      exercises.push({
        id: newId(),
        exerciseId: e.exerciseId,
        name: e.name,
        trackingType: e.trackingType,
        primaryMuscle: e.primaryMuscle,
        secondaryMuscles: e.secondaryMuscles,
        blockIndex,
        method: block.method,
        position,
        status: "done",
        substitutedFrom: null,
        restSeconds: e.restSeconds,
        notes: e.notes,
        sets: plannedSets.map((s, i) => ({
          id: newId(),
          setNumber: i + 1,
          type: s.type,
          planned: s,
          // pré-preenchido: o topo da faixa de reps e a carga planejada
          reps: s.repsMax ?? s.repsMin,
          loadKg: s.loadKg,
          durationSeconds: s.durationSeconds,
          distanceM: s.distanceM,
          rir: null,
          completed: false,
          completedAt: null,
        })),
      });
    }
  });
  const started = iso(now);
  return {
    id: newId(),
    linkId: planned.linkId,
    programId: planned.programId,
    workoutId: planned.workoutId,
    workoutVersionId: planned.versionId,
    workoutName: planned.name,
    performedBy,
    status: "in_progress",
    startedAt: started,
    finishedAt: null,
    clientUpdatedAt: started,
    blocks: planned.blocks.map((b) => ({
      method: b.method,
      preset: b.preset,
      rounds: b.rounds,
      workSeconds: b.workSeconds,
      restSeconds: b.restSeconds,
      restBetweenRounds: b.restBetweenRounds,
      durationSeconds: b.durationSeconds,
      name: b.name,
    })),
    exercises,
    feedback: null,
    syncStatus: "pending",
  };
}

function touch(session: Session, now: Date, change: Partial<Session>): Session {
  return { ...session, ...change, clientUpdatedAt: iso(now), syncStatus: "pending" };
}

function mapExercise(
  session: Session,
  exerciseId: string,
  now: Date,
  change: (e: SessionExercise) => SessionExercise,
): Session {
  return touch(session, now, {
    exercises: session.exercises.map((e) => (e.id === exerciseId ? change(e) : e)),
  });
}

export const act = {
  /** Confirma (ou ajusta) uma série. Desmarcar também vale: série feita sem querer. */
  setSet(
    session: Session,
    exerciseId: string,
    setId: string,
    values: Partial<
      Pick<SessionSet, "reps" | "loadKg" | "durationSeconds" | "distanceM" | "rir" | "completed">
    >,
    now: Date,
  ): Session {
    return mapExercise(session, exerciseId, now, (e) => ({
      ...e,
      status: e.status === "skipped" ? "done" : e.status,
      sets: e.sets.map((s) =>
        s.id === setId
          ? {
              ...s,
              ...values,
              completedAt:
                values.completed === undefined ? s.completedAt : values.completed ? iso(now) : null,
            }
          : s,
      ),
    }));
  },
  addSet(session: Session, exerciseId: string, now: Date, newId: NewId): Session {
    return mapExercise(session, exerciseId, now, (e) => {
      const last = e.sets.at(-1);
      return {
        ...e,
        sets: [
          ...e.sets,
          {
            id: newId(),
            setNumber: e.sets.length + 1,
            type: "normal",
            planned: null,
            reps: last?.reps ?? null,
            loadKg: last?.loadKg ?? null,
            durationSeconds: last?.durationSeconds ?? null,
            distanceM: last?.distanceM ?? null,
            rir: null,
            completed: false,
            completedAt: null,
          },
        ],
      };
    });
  },
  skip(session: Session, exerciseId: string, now: Date): Session {
    return mapExercise(session, exerciseId, now, (e) => ({ ...e, status: "skipped" }));
  },
  /** Troca o exercício (aparelho ocupado, dor): guarda qual era o planejado. */
  substitute(
    session: Session,
    exerciseId: string,
    replacement: Pick<
      SessionExercise,
      "exerciseId" | "name" | "trackingType" | "primaryMuscle" | "secondaryMuscles"
    >,
    now: Date,
  ): Session {
    return mapExercise(session, exerciseId, now, (e) => ({
      ...e,
      ...replacement,
      status: "substituted",
      substitutedFrom: e.substitutedFrom ?? e.exerciseId,
    }));
  },
  finish(session: Session, feedback: Feedback, now: Date): Session {
    return touch(session, now, {
      status: isComplete(session) ? "completed" : "partial",
      finishedAt: session.finishedAt ?? iso(now),
      feedback,
    });
  },
  discard(session: Session, now: Date): Session {
    return touch(session, now, { status: "abandoned", finishedAt: iso(now) });
  },
};

/** Tudo feito: toda série dos exercícios não pulados confirmada. */
export function isComplete(session: Session): boolean {
  return (
    session.exercises.every((e) => e.status === "skipped" || e.sets.every((s) => s.completed)) &&
    session.exercises.some((e) => e.status !== "skipped")
  );
}

/** 0 a 1, para a adesão: séries confirmadas / séries planejadas. */
export function completionRatio(session: Session): number {
  const sets = session.exercises.flatMap((e) => e.sets);
  if (sets.length === 0) {
    return 0;
  }
  return Math.round((sets.filter((s) => s.completed).length / sets.length) * 1000) / 1000;
}

export function durationSeconds(session: Session): number | null {
  if (session.finishedAt === null) {
    return null;
  }
  return Math.max(
    0,
    Math.round((Date.parse(session.finishedAt) - Date.parse(session.startedAt)) / 1000),
  );
}

/** Volume: soma de carga × repetições das séries feitas (kg). */
export function volumeKg(session: Session): number {
  let total = 0;
  for (const e of session.exercises) {
    for (const s of e.sets) {
      if (s.completed && s.loadKg !== null && s.reps !== null) {
        total += s.loadKg * s.reps;
      }
    }
  }
  return Math.round(total * 10) / 10;
}

/** Músculos trabalhados (para o mapa): principal 2, secundário 1, só dos exercícios feitos. */
export function musclesWorked(session: Session): Record<string, 1 | 2> {
  const levels: Record<string, 1 | 2> = {};
  for (const e of session.exercises) {
    if (e.status === "skipped" || !e.sets.some((s) => s.completed)) {
      continue;
    }
    for (const m of e.secondaryMuscles) {
      levels[m] = levels[m] === 2 ? 2 : 1;
    }
    if (e.primaryMuscle !== null) {
      levels[e.primaryMuscle] = 2;
    }
  }
  return levels;
}

export interface Comparison {
  readonly exerciseId: string;
  readonly name: string;
  /** Maior carga feita hoje e na vez anterior. */
  readonly topLoadKg: number | null;
  readonly previousTopLoadKg: number | null;
}

/** Comparação com a última sessão do mesmo treino (o que mudou na maior carga). */
export function compare(session: Session, previous: Session | null): readonly Comparison[] {
  const top = (s: Session, exerciseId: string) => {
    const loads = s.exercises
      .filter((e) => e.exerciseId === exerciseId)
      .flatMap((e) => e.sets)
      .filter((set) => set.completed && set.loadKg !== null)
      .map((set) => set.loadKg ?? 0);
    return loads.length === 0 ? null : Math.max(...loads);
  };
  return session.exercises
    .filter((e) => e.status !== "skipped")
    .map((e) => ({
      exerciseId: e.exerciseId,
      name: e.name,
      topLoadKg: top(session, e.exerciseId),
      previousTopLoadKg: previous === null ? null : top(previous, e.exerciseId),
    }));
}

/** O que vai no POST /v1/sync (o servidor decide quem registrou pela conta). */
export function toSyncPayload(session: Session) {
  return {
    id: session.id,
    linkId: session.linkId,
    programId: session.programId,
    workoutId: session.workoutId,
    workoutVersionId: session.workoutVersionId,
    status: session.status,
    startedAt: session.startedAt,
    finishedAt: session.finishedAt,
    durationSeconds: durationSeconds(session),
    completionRatio: completionRatio(session),
    clientUpdatedAt: session.clientUpdatedAt,
    exercises: session.exercises.map((e) => ({
      id: e.id,
      exerciseId: e.exerciseId,
      position: e.position,
      status: e.status,
      substitutedFrom: e.substitutedFrom,
      notes: e.notes,
      sets: e.sets.map((s) => ({
        id: s.id,
        setNumber: s.setNumber,
        setType: s.type,
        reps: s.reps,
        loadKg: s.loadKg,
        durationSeconds: s.durationSeconds,
        distanceM: s.distanceM,
        rir: s.rir,
        completed: s.completed,
        completedAt: s.completedAt,
      })),
    })),
    feedback:
      session.feedback === null
        ? null
        : {
            effort: session.feedback.effort,
            comment: session.feedback.comment,
            pains: session.feedback.pains.map((p) => ({
              id: p.id,
              bodyRegion: p.bodyRegion,
              exerciseId: p.exerciseId,
              intensity: p.intensity,
            })),
          },
  };
}
