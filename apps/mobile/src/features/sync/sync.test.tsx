import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, render, renderHook, screen, waitFor } from "@testing-library/react-native";
import type { ReactNode } from "react";
import { RepositoriesProvider, type Repositories } from "../../providers/repositories";
import { createMemoryStore } from "./data/memory-store";
import type { PlannedChanges, PlannedWorkout } from "./domain/planned";
import { todayOf } from "./domain/planned";
import { usePlannedSnapshot, useSyncPlanned } from "./hooks/use-planned";
import { TodayCard } from "./ui/TodayCard";

jest.mock("expo-router", () => ({ router: { push: jest.fn() } }));

const workout = (id: string, position: number, weekdays: number[]): PlannedWorkout => ({
  id,
  programId: "p1",
  name: `Treino ${String.fromCharCode(64 + position)}`,
  position,
  weekdays,
  versionId: `v-${id}`,
  versionNumber: 1,
  goal: position === 1 ? "Peito e tríceps" : null,
  estimatedMinutes: 50,
  notes: null,
  blocks: [
    {
      name: null,
      method: "sequential",
      preset: null,
      rounds: null,
      workSeconds: null,
      restSeconds: null,
      restBetweenRounds: null,
      durationSeconds: null,
      exercises: [{ exerciseId: "bench", restSeconds: null, notes: null, sets: [] }],
    },
  ],
});

const program = {
  id: "p1",
  linkId: "l1",
  name: "Hipertrofia",
  goal: null,
  startsOn: null,
  endsOn: null,
  scheduleMode: "fixed_days" as const,
  weeklyTarget: null,
};

const changes: PlannedChanges = {
  cursor: "2026-10-05T12:00:00Z",
  programs: [
    { program, deleted: false, workouts: [workout("a", 1, [1, 4]), workout("b", 2, [2, 5])] },
  ],
  exercises: [
    {
      id: "bench",
      name: "Supino reto com barra",
      trackingType: "reps_load",
      primaryMuscle: "chest",
      secondaryMuscles: ["triceps"],
      equipment: "barra",
      instructions: null,
      mediaUrl: null,
    },
  ],
};

// 5/10/2026 é segunda; 7/10 é quarta (sem treino)
const MONDAY = new Date(2026, 9, 5, 9);
const WEDNESDAY = new Date(2026, 9, 7, 9);

describe("planejado no aparelho", () => {
  it("dias fixos: o do dia, ou o próximo dia com treino", async () => {
    const store = createMemoryStore();
    await store.apply(changes);
    const snapshot = await store.snapshot();

    expect(todayOf(snapshot, MONDAY)).toMatchObject({ kind: "workout", workout: { id: "a" } });
    expect(todayOf(snapshot, WEDNESDAY)).toMatchObject({
      kind: "rest",
      next: { id: "a" },
      nextWeekday: 4,
    });
  });

  it("sequência: o próximo depois do último feito, voltando ao início", async () => {
    const store = createMemoryStore();
    await store.apply({
      ...changes,
      programs: [
        {
          program: { ...program, scheduleMode: "sequence", weeklyTarget: 3 },
          deleted: false,
          workouts: [workout("a", 1, []), workout("b", 2, [])],
        },
      ],
    });
    const snapshot = await store.snapshot();

    expect(todayOf(snapshot, MONDAY)).toMatchObject({ kind: "next", workout: { id: "a" } });
    expect(todayOf(snapshot, MONDAY, "a")).toMatchObject({ workout: { id: "b" } });
    expect(todayOf(snapshot, MONDAY, "b")).toMatchObject({ workout: { id: "a" } });
  });

  it("aplicar de novo não duplica; treino removido some; tombstone apaga o programa", async () => {
    const store = createMemoryStore();
    await store.apply(changes);
    await store.apply(changes);
    expect((await store.snapshot()).workouts).toHaveLength(2);

    await store.apply({
      ...changes,
      programs: [{ program, deleted: false, workouts: [workout("a", 1, [1])] }],
    });
    expect((await store.snapshot()).workouts.map((w) => w.id)).toEqual(["a"]);

    await store.apply({ ...changes, programs: [{ program, deleted: true, workouts: [] }] });
    const empty = await store.snapshot();
    expect(empty.program).toBeNull();
    expect(empty.workouts).toEqual([]);
    expect(await store.cursor()).toBe(changes.cursor);
  });

  it("baixa com o cursor salvo e mostra o card do dia", async () => {
    const store = createMemoryStore();
    const changesSince = jest.fn(() => Promise.resolve(changes));
    const repositories = {
      syncApi: { changesSince },
      plannedStore: store,
    } as unknown as Repositories;
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false, gcTime: Number.POSITIVE_INFINITY },
        mutations: { gcTime: Number.POSITIVE_INFINITY },
      },
    });
    function wrapper({ children }: { readonly children: ReactNode }) {
      return (
        <QueryClientProvider client={queryClient}>
          <RepositoriesProvider repositories={repositories}>{children}</RepositoriesProvider>
        </QueryClientProvider>
      );
    }
    const { result } = await renderHook(
      () => ({ sync: useSyncPlanned(), snapshot: usePlannedSnapshot() }),
      { wrapper },
    );

    await act(() => result.current.sync.mutateAsync());
    await act(() => result.current.sync.mutateAsync());
    expect(changesSince).toHaveBeenNthCalledWith(1, null);
    expect(changesSince).toHaveBeenNthCalledWith(2, changes.cursor);
    await waitFor(() => {
      expect(result.current.snapshot.data?.workouts).toHaveLength(2);
    });

    const snapshot = result.current.snapshot.data;
    if (snapshot === undefined) {
      throw new Error("sem snapshot");
    }
    await render(<TodayCard snapshot={snapshot} date={MONDAY} />);
    expect(screen.getByRole("header", { name: "PEITO E TRÍCEPS" })).toBeOnTheScreen();
    expect(screen.getByText("Treino A · 1 exercício · ~50 min")).toBeOnTheScreen();
  });
});
