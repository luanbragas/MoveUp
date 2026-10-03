import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react-native";
import { SafeAreaProvider } from "react-native-safe-area-context";
import { RepositoriesProvider, type Repositories } from "../../../providers/repositories";
import { ApiFailure } from "../../../shared/lib/http";
import { defaultSets, type Workout } from "../domain/workout";
import { WorkoutEditor } from "./WorkoutEditorScreen";

jest.mock("expo-router", () => ({ router: { back: jest.fn(), push: jest.fn() } }));

const METRICS = {
  frame: { x: 0, y: 0, width: 390, height: 844 },
  insets: { top: 0, left: 0, right: 0, bottom: 0 },
};

const WORKOUT: Workout = {
  id: "w1",
  template: true,
  programId: null,
  revision: 3,
  versionId: "v1",
  versionNumber: 1,
  draft: {
    name: "Treino A",
    goal: "Peito",
    estimatedMinutes: 50,
    notes: null,
    blocks: [
      {
        key: "b1",
        name: null,
        method: "sequential",
        preset: null,
        rounds: null,
        workSeconds: null,
        restSeconds: null,
        restBetweenRounds: null,
        durationSeconds: null,
        exercises: [
          {
            key: "e1",
            exerciseId: "bench",
            exerciseName: "Supino reto com barra",
            trackingType: "reps_load",
            primaryMuscle: "chest",
            restSeconds: null,
            notes: null,
            sets: defaultSets("reps_load").map((s) => ({ ...s, loadKg: 55, restSeconds: 90 })),
          },
        ],
      },
    ],
  },
};

async function renderEditor(saveWorkout: jest.Mock, reload = jest.fn(() => Promise.resolve({}))) {
  const repositories = { training: { saveWorkout } } as unknown as Repositories;
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { gcTime: Number.POSITIVE_INFINITY },
      mutations: { gcTime: Number.POSITIVE_INFINITY },
    },
  });
  await render(
    <SafeAreaProvider initialMetrics={METRICS}>
      <QueryClientProvider client={queryClient}>
        <RepositoriesProvider repositories={repositories}>
          <WorkoutEditor workout={WORKOUT} reload={reload} />
        </RepositoriesProvider>
      </QueryClientProvider>
    </SafeAreaProvider>,
  );
}

describe("editor de treino", () => {
  it("mostra o resumo e salva com a revisão lida", async () => {
    const saveWorkout = jest.fn(() => Promise.resolve({ ...WORKOUT, revision: 4 }));
    await renderEditor(saveWorkout);

    expect(screen.getByText("3 séries · 8 a 12 reps · 55 kg · desc. 90 s")).toBeOnTheScreen();
    expect(screen.getByRole("button", { name: "Salvar" })).toBeDisabled();

    await fireEvent.changeText(screen.getByLabelText("Nome do treino"), "Treino A · peito");
    await fireEvent.press(screen.getByRole("button", { name: "Salvar" }));

    await waitFor(() => {
      expect(saveWorkout).toHaveBeenCalledWith(
        "w1",
        3,
        expect.objectContaining({ name: "Treino A · peito" }),
      );
    });
    expect(await screen.findByText("Salvo · versão 1")).toBeOnTheScreen();
  });

  it("não deixa salvar com bloco vazio e explica o porquê", async () => {
    await renderEditor(jest.fn());

    await fireEvent.press(screen.getByRole("button", { name: "Adicionar bloco" }));
    await fireEvent.press(screen.getByRole("button", { name: /Biset \/ triset/ }));

    expect(screen.getAllByText("Tem bloco sem exercício.").length).toBeGreaterThan(0);
    expect(screen.getByRole("button", { name: "Salvar" })).toBeDisabled();
  });

  it("salvo em outro aparelho: oferece usar a versão salva", async () => {
    const saveWorkout = jest.fn(() =>
      Promise.reject(
        new ApiFailure({ kind: "problem", code: "version-mismatch", status: 412, traceId: "t" }),
      ),
    );
    const reload = jest.fn(() => Promise.resolve({}));
    await renderEditor(saveWorkout, reload);

    await fireEvent.changeText(screen.getByLabelText("Nome do treino"), "Outro nome");
    await fireEvent.press(screen.getByRole("button", { name: "Salvar" }));

    await fireEvent.press(await screen.findByRole("button", { name: "Usar a versão salva" }));
    await waitFor(() => {
      expect(reload).toHaveBeenCalled();
    });
    // o editor fica na tela de conflito até a versão salva chegar (a rota recria o editor)
    expect(screen.getByText(/Alterado em/)).toBeOnTheScreen();
  });
});
