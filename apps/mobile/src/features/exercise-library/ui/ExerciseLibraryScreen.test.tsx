import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react-native";
import { SafeAreaProvider } from "react-native-safe-area-context";
import { RepositoriesProvider, type Repositories } from "../../../providers/repositories";
import { muscleLevels, type Exercise, type MuscleCode } from "../domain/exercise";
import { ExerciseLibraryScreen } from "./ExerciseLibraryScreen";

jest.mock("expo-router", () => ({ router: { back: jest.fn(), push: jest.fn() } }));

const exercise = (id: string, name: string, primary: MuscleCode, secondary: MuscleCode[] = []) =>
  ({
    id,
    name,
    modality: "strength",
    trackingType: "reps_load",
    primaryMuscle: primary,
    secondaryMuscles: secondary,
    equipment: "barra",
    unilateral: false,
    instructions: "Desça a barra até o peito.",
    mediaUrl: null,
    custom: false,
  }) satisfies Exercise;

const LIBRARY = [
  exercise("1", "Supino reto com barra", "chest", ["triceps", "delts"]),
  exercise("2", "Supino inclinado com barra", "chest", ["delts"]),
  exercise("3", "Mesa flexora", "hamstrings"),
];

const METRICS = {
  frame: { x: 0, y: 0, width: 390, height: 844 },
  insets: { top: 0, left: 0, right: 0, bottom: 0 },
};

async function renderLibrary(onPick?: (picked: readonly Exercise[]) => void) {
  const search = jest.fn((query: string, muscle: MuscleCode | null) =>
    Promise.resolve(
      LIBRARY.filter(
        (e) =>
          e.name.toLowerCase().includes(query.toLowerCase()) &&
          (muscle === null || e.primaryMuscle === muscle),
      ),
    ),
  );
  const repositories = {
    exercises: { search, create: jest.fn(), archive: jest.fn() },
  } as unknown as Repositories;
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: Number.POSITIVE_INFINITY } },
  });
  await render(
    <SafeAreaProvider initialMetrics={METRICS}>
      <QueryClientProvider client={queryClient}>
        <RepositoriesProvider repositories={repositories}>
          <ExerciseLibraryScreen {...(onPick === undefined ? {} : { onPick })} />
        </RepositoriesProvider>
      </QueryClientProvider>
    </SafeAreaProvider>,
  );
  return search;
}

describe("biblioteca de exercícios", () => {
  it("principal pesa 2 e secundários 1 no mapa muscular", () => {
    expect(muscleLevels(LIBRARY[0] as Exercise)).toEqual({ chest: 2, triceps: 1, delts: 1 });
  });

  it("filtra por músculo e abre o como fazer", async () => {
    const search = await renderLibrary();
    await screen.findByText("Mesa flexora");

    await fireEvent.press(screen.getByRole("radio", { name: "Posteriores" }));
    await waitFor(() => {
      expect(screen.queryByText("Supino reto com barra")).toBeNull();
    });
    expect(search).toHaveBeenLastCalledWith("", "hamstrings");

    await fireEvent.press(screen.getByRole("button", { name: /Mesa flexora/ }));
    expect(screen.getByText("Como fazer")).toBeOnTheScreen();
  });

  it("no editor, marca vários e devolve na ordem escolhida", async () => {
    const onPick = jest.fn();
    await renderLibrary(onPick);
    await screen.findByText("Mesa flexora");
    expect(screen.getByRole("button", { name: "Escolha os exercícios" })).toBeDisabled();

    await fireEvent.press(screen.getByRole("checkbox", { name: "Mesa flexora" }));
    await fireEvent.press(screen.getByRole("checkbox", { name: "Supino reto com barra" }));
    await fireEvent.press(screen.getByRole("button", { name: "Adicionar 2 exercícios" }));

    expect(onPick).toHaveBeenCalledWith([LIBRARY[2], LIBRARY[0]]);
  });
});
