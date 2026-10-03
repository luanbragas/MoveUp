import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react-native";
import { Alert } from "react-native";
import { SafeAreaProvider } from "react-native-safe-area-context";
import { RepositoriesProvider, type Repositories } from "../../../providers/repositories";
import { createMemorySessionStore } from "../data/session-stores";
import { startSession, type PlannedInput } from "../domain/session";
import { ExecutionScreen } from "./ExecutionScreen";

jest.setTimeout(20_000);
jest.mock("expo-router", () => ({ router: { back: jest.fn(), push: jest.fn() } }));

const METRICS = {
  frame: { x: 0, y: 0, width: 390, height: 844 },
  insets: { top: 0, left: 0, right: 0, bottom: 0 },
};

const set = (reps: number, kg: number) => ({
  type: "normal",
  repsMin: reps,
  repsMax: reps,
  loadKg: kg,
  durationSeconds: null,
  distanceM: null,
  restSeconds: 90,
});

const PLANNED: PlannedInput = {
  linkId: "link",
  programId: null,
  workoutId: "w",
  versionId: "v",
  name: "Treino A",
  blocks: [
    {
      method: "superset",
      preset: null,
      rounds: null,
      workSeconds: null,
      restSeconds: null,
      restBetweenRounds: 60,
      durationSeconds: null,
      name: null,
      exercises: [
        {
          exerciseId: "a",
          name: "Rosca direta",
          trackingType: "reps_load",
          primaryMuscle: "biceps",
          secondaryMuscles: [],
          restSeconds: null,
          notes: null,
          sets: [set(10, 20)],
        },
        {
          exerciseId: "b",
          name: "Tríceps corda",
          trackingType: "reps_load",
          primaryMuscle: "triceps",
          secondaryMuscles: [],
          restSeconds: null,
          notes: null,
          sets: [set(12, 15)],
        },
      ],
    },
  ],
};

const restAlarm = {
  schedule: jest.fn(() => Promise.resolve("alarm-1")),
  cancel: jest.fn(() => Promise.resolve()),
};

let n = 0;
const newId = () => {
  n += 1;
  return `id-${String(n)}`;
};

async function renderRun() {
  const store = createMemorySessionStore();
  const repositories = {
    sessionStore: store,
    sessionSyncApi: { push: () => Promise.resolve({ written: [], unchanged: [] }) },
    restAlarm,
  } as unknown as Repositories;
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { gcTime: Number.POSITIVE_INFINITY, retry: false },
      mutations: { gcTime: Number.POSITIVE_INFINITY },
    },
  });
  const session = startSession(PLANNED, "client", new Date(), newId);
  const onClose = jest.fn();
  await render(
    <SafeAreaProvider initialMetrics={METRICS}>
      <QueryClientProvider client={queryClient}>
        <RepositoriesProvider repositories={repositories}>
          <ExecutionScreen
            session={session}
            exercises={new Map()}
            firstName="Bia"
            onClose={onClose}
          />
        </RepositoriesProvider>
      </QueryClientProvider>
    </SafeAreaProvider>,
  );
  return { store, onClose };
}

describe("execução do treino", () => {
  it("biset: confirma e vai para o próximo do grupo; no fim da volta, descanso", async () => {
    const { store } = await renderRun();
    expect(screen.getByRole("header", { name: "Rosca direta" })).toBeOnTheScreen();

    await fireEvent.press(screen.getByRole("button", { name: "Confirmar série 1" }));
    expect(await screen.findByRole("header", { name: "Tríceps corda" })).toBeOnTheScreen();
    expect(screen.queryByText("Descanso")).toBeNull();

    await fireEvent.press(screen.getByRole("button", { name: "Confirmar série 1" }));
    expect(await screen.findByText("Descanso")).toBeOnTheScreen();
    // aviso do fim agendado para tocar com a tela bloqueada; pular cancela
    await waitFor(() => {
      expect(restAlarm.schedule).toHaveBeenCalledTimes(1);
    });
    await fireEvent.press(screen.getByRole("button", { name: "Pular descanso" }));
    expect(restAlarm.cancel).toHaveBeenCalledWith("alarm-1");

    // cada toque já está no aparelho
    await waitFor(async () => {
      const saved = await store.active();
      expect(saved?.exercises.every((e) => e.sets.every((s) => s.completed))).toBe(true);
    });
  });

  it("finaliza: feedback com esforço e o resumo com o volume", async () => {
    jest.spyOn(Alert, "alert").mockImplementation((_t, _m, buttons) => {
      buttons?.at(-1)?.onPress?.();
    });
    const { store, onClose } = await renderRun();

    await fireEvent.press(screen.getByRole("button", { name: "Finalizar" }));
    expect(await screen.findByRole("button", { name: "Concluir treino" })).toBeDisabled();
    await fireEvent.press(screen.getByRole("radio", { name: "Esforço 7 de 10" }));
    await fireEvent.press(screen.getByRole("button", { name: "Concluir treino" }));

    expect(await screen.findByText("volume")).toBeOnTheScreen();
    expect((await store.history())[0]).toMatchObject({
      status: "partial",
      feedback: { effort: 7 },
    });
    await fireEvent.press(screen.getByRole("button", { name: "Concluir" }));
    expect(onClose).toHaveBeenCalled();
  });
});
