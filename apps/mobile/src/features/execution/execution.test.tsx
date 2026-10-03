import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act as rtlAct, renderHook } from "@testing-library/react-native";
import type { ReactNode } from "react";
import { RepositoriesProvider, type Repositories } from "../../providers/repositories";
import { ApiFailure } from "../../shared/lib/http";
import { uuidV7 } from "../../shared/lib/uuid-v7";
import { createMemorySessionStore } from "./data/session-stores";
import {
  act,
  compare,
  completionRatio,
  isComplete,
  musclesWorked,
  startSession,
  toSyncPayload,
  volumeKg,
  type PlannedInput,
} from "./domain/session";
import { usePushSessions } from "./hooks/use-sessions";

let counter = 0;
const newId = () => {
  counter += 1;
  return `id-${String(counter)}`;
};

const PLANNED: PlannedInput = {
  linkId: "link",
  programId: "program",
  workoutId: "workout-a",
  versionId: "version-1",
  name: "Treino A",
  blocks: [
    {
      method: "sequential",
      preset: null,
      rounds: null,
      workSeconds: null,
      restSeconds: null,
      restBetweenRounds: null,
      durationSeconds: null,
      name: null,
      exercises: [
        {
          exerciseId: "bench",
          name: "Supino",
          trackingType: "reps_load",
          primaryMuscle: "chest",
          secondaryMuscles: ["triceps"],
          restSeconds: 90,
          notes: null,
          sets: [1, 2].map(() => ({
            type: "normal",
            repsMin: 8,
            repsMax: 10,
            loadKg: 55,
            durationSeconds: null,
            distanceM: null,
            restSeconds: 90,
          })),
        },
        {
          exerciseId: "fly",
          name: "Crucifixo",
          trackingType: "reps_load",
          primaryMuscle: "chest",
          secondaryMuscles: ["delts"],
          restSeconds: 60,
          notes: "Pegue leve",
          sets: [
            {
              type: "normal",
              repsMin: 12,
              repsMax: 12,
              loadKg: 14,
              durationSeconds: null,
              distanceM: null,
              restSeconds: 60,
            },
          ],
        },
      ],
    },
  ],
};

const T0 = new Date("2026-10-05T12:00:00Z");
const at = (minutes: number) => new Date(T0.getTime() + minutes * 60_000);

function doAll(session = startSession(PLANNED, "client", T0, newId)) {
  let s = session;
  for (const e of s.exercises) {
    for (const set of e.sets) {
      s = act.setSet(s, e.id, set.id, { completed: true }, at(10));
    }
  }
  return s;
}

describe("treino no aparelho", () => {
  it("UUIDv7: versão 7, variante RFC e ordenado pelo tempo", () => {
    const a = uuidV7(1_700_000_000_000, () => new Uint8Array(16).fill(255));
    const b = uuidV7(1_700_000_000_001, () => new Uint8Array(16).fill(0));
    expect(a).toMatch(/^[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/);
    expect(a < b).toBe(true);
  });

  it("inicia com as séries pré-preenchidas pelo planejado", () => {
    const session = startSession(PLANNED, "client", T0, newId);
    const [bench, fly] = session.exercises;

    expect(session.status).toBe("in_progress");
    expect(session.workoutVersionId).toBe("version-1");
    expect(bench?.sets.map((s) => [s.reps, s.loadKg, s.completed])).toEqual([
      [10, 55, false],
      [10, 55, false],
    ]);
    expect(fly?.notes).toBe("Pegue leve");
    expect(session.syncStatus).toBe("pending");
  });

  it("finaliza completo ou parcial, com volume, músculos e conclusão", () => {
    const partial = act.finish(
      act.setSet(startSession(PLANNED, "client", T0, newId), "x", "y", { completed: true }, at(5)),
      { effort: 6, comment: null, pains: [] },
      at(30),
    );
    expect(partial.status).toBe("partial");

    const full = act.finish(doAll(), { effort: 8, comment: "Bom", pains: [] }, at(50));
    expect(isComplete(full)).toBe(true);
    expect(full.status).toBe("completed");
    expect(completionRatio(full)).toBe(1);
    expect(volumeKg(full)).toBe(55 * 10 * 2 + 14 * 12);
    expect(musclesWorked(full)).toEqual({ chest: 2, triceps: 1, delts: 1 });
    expect(toSyncPayload(full)).toMatchObject({
      status: "completed",
      durationSeconds: 50 * 60,
      completionRatio: 1,
      feedback: { effort: 8, comment: "Bom" },
    });
  });

  it("pular e trocar guardam o que era o planejado", () => {
    let s = startSession(PLANNED, "client", T0, newId);
    const [bench, fly] = s.exercises;
    s = act.skip(s, fly?.id ?? "", at(1));
    s = act.substitute(
      s,
      bench?.id ?? "",
      {
        exerciseId: "dumbbell-bench",
        name: "Supino halteres",
        trackingType: "reps_load",
        primaryMuscle: "chest",
        secondaryMuscles: [],
      },
      at(2),
    );

    expect(s.exercises[1]?.status).toBe("skipped");
    expect(s.exercises[0]).toMatchObject({
      status: "substituted",
      exerciseId: "dumbbell-bench",
      substitutedFrom: "bench",
    });
  });

  it("bloco por tempo: rodadas no resultado do bloco, não nas reps (não vira volume)", () => {
    const amrap: PlannedInput = {
      ...PLANNED,
      blocks: PLANNED.blocks.map((b) => ({ ...b, method: "amrap", durationSeconds: 600 })),
    };
    let s = startSession(amrap, "client", T0, newId);
    s = act.blockDone(
      s,
      { blockIndex: 0, roundsCompleted: 5, extraReps: 3, totalSeconds: 600 },
      at(12),
    );
    s = act.blockDone(
      s,
      { blockIndex: 0, roundsCompleted: 6, extraReps: 0, totalSeconds: 600 },
      at(13),
    );

    expect(isComplete(s)).toBe(true);
    expect(s.exercises[0]?.sets[0]).toMatchObject({ reps: 10, durationSeconds: 600 });
    expect(toSyncPayload(s).blockResults).toEqual([
      { blockIndex: 0, roundsCompleted: 6, extraReps: 0, totalSeconds: 600 },
    ]);
  });

  it("compara a maior carga com a última vez do mesmo treino", () => {
    const before = act.finish(doAll(), { effort: 7, comment: null, pains: [] }, at(40));
    let today = startSession(PLANNED, "client", at(60 * 24 * 3), newId);
    const bench = today.exercises[0];
    for (const set of bench?.sets ?? []) {
      today = act.setSet(
        today,
        bench?.id ?? "",
        set.id,
        { completed: true, loadKg: 60 },
        at(60 * 24 * 3 + 5),
      );
    }
    const rows = compare(today, before);
    expect(rows[0]).toMatchObject({ name: "Supino", topLoadKg: 60, previousTopLoadKg: 55 });
  });

  it("offline: fica pendente; a rede volta e vai uma vez só; edição durante o envio continua pendente", async () => {
    const store = createMemorySessionStore();
    let online = false;
    const received: string[] = [];
    const push = jest.fn((sessions: readonly { id: string }[]) => {
      if (!online) {
        return Promise.reject(new ApiFailure({ kind: "network" }));
      }
      sessions.forEach((s) => received.push(s.id));
      return Promise.resolve({ written: sessions.map((s) => s.id), unchanged: [] });
    });
    const repositories = {
      sessionStore: store,
      sessionSyncApi: { push },
    } as unknown as Repositories;
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false, gcTime: Number.POSITIVE_INFINITY },
        mutations: { retry: false, gcTime: Number.POSITIVE_INFINITY, networkMode: "always" },
      },
    });
    function wrapper({ children }: { readonly children: ReactNode }) {
      return (
        <QueryClientProvider client={queryClient}>
          <RepositoriesProvider repositories={repositories}>{children}</RepositoriesProvider>
        </QueryClientProvider>
      );
    }
    const { result } = await renderHook(() => usePushSessions(), { wrapper });

    // treino inteiro registrado sem rede ("o app é fechado": só o SQLite guarda)
    const session = act.finish(doAll(), { effort: 7, comment: null, pains: [] }, at(45));
    await store.save(session);
    await rtlAct(async () => {
      await result.current.mutateAsync().catch(() => undefined);
    });
    expect(await store.pending()).toHaveLength(1);

    // a rede volta: vai uma vez; reenviar não manda de novo
    online = true;
    await rtlAct(() => result.current.mutateAsync());
    await rtlAct(() => result.current.mutateAsync());
    expect(received).toEqual([session.id]);
    expect(await store.pending()).toHaveLength(0);

    // correção depois de enviada volta a pendente e vai de novo (o servidor marca a edição)
    const fixed = act.setSet(
      session,
      session.exercises[0]?.id ?? "",
      session.exercises[0]?.sets[0]?.id ?? "",
      { reps: 9 },
      at(60),
    );
    await store.save(fixed);
    expect(await store.pending()).toHaveLength(1);
    await store.markSynced([{ id: fixed.id, clientUpdatedAt: session.clientUpdatedAt }]);
    expect(await store.pending()).toHaveLength(1); // marcar com o carimbo antigo não apaga a edição
    await rtlAct(() => result.current.mutateAsync());
    expect(received).toEqual([session.id, session.id]);
    expect(await store.pending()).toHaveLength(0);
  });
});
