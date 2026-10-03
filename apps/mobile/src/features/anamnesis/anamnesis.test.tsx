import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react-native";
import type { ReactNode } from "react";
import { SafeAreaProvider } from "react-native-safe-area-context";
import { RepositoriesProvider, type Repositories } from "../../providers/repositories";
import { createMemoryAnamnesisDraft } from "./data/draft-store";
import { createFakeAnamnesis, FAKE_TEMPLATE } from "./data/fakes/fake-anamnesis";
import {
  missing,
  parqPositive,
  restrictionsFor,
  toggleOption,
  withAnswer,
  type Restriction,
} from "./domain/anamnesis";
import { AnamnesisBanner } from "./ui/AnamnesisBanner";
import { ClientAnamnesisScreen } from "./ui/ClientAnamnesisScreen";
import { RestrictionWarning } from "./ui/RestrictionBanner";

jest.mock("expo-router", () => ({ router: { back: jest.fn(), push: jest.fn() } }));
jest.setTimeout(20_000);

const METRICS = {
  frame: { x: 0, y: 0, width: 390, height: 844 },
  insets: { top: 0, left: 0, right: 0, bottom: 0 },
};

const KNEE: Restriction = {
  id: "r1",
  kind: "injury",
  bodyRegion: "knee_left",
  description: "Lesão no menisco",
  severity: 2,
  resolvedOn: null,
  fromAnamnesis: false,
};

async function renderWith(
  ui: ReactNode,
  repo = createFakeAnamnesis(),
  draft = createMemoryAnamnesisDraft(),
) {
  const repositories = { anamnesis: repo, anamnesisDraft: draft } as unknown as Repositories;
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { gcTime: Number.POSITIVE_INFINITY, retry: false },
      mutations: { gcTime: Number.POSITIVE_INFINITY },
    },
  });
  await render(
    <SafeAreaProvider initialMetrics={METRICS}>
      <QueryClientProvider client={queryClient}>
        <RepositoriesProvider repositories={repositories}>{ui}</RepositoriesProvider>
      </QueryClientProvider>
    </SafeAreaProvider>,
  );
  return { repo, draft };
}

describe("anamnese", () => {
  it("regras: obrigatórias, PAR-Q e respostas", () => {
    expect(missing(FAKE_TEMPLATE, {}).map((q) => q.code)).toEqual([
      "goal",
      "weekly_days",
      "parq_heart",
    ]);
    expect(parqPositive(FAKE_TEMPLATE, { parq_heart: true })).toBe(true);
    expect(toggleOption(["a"], "a")).toEqual([]);
    expect(withAnswer({ a: 1, b: 2 }, "a", undefined)).toEqual({ b: 2 });
  });

  it("restrição avisa só nos músculos da região, e só enquanto ativa", () => {
    expect(restrictionsFor("quads", [KNEE])).toEqual([KNEE]);
    expect(restrictionsFor("chest", [KNEE])).toEqual([]);
    expect(restrictionsFor("quads", [{ ...KNEE, resolvedOn: "2026-10-01" }])).toEqual([]);
  });

  it("passo a passo: trava sem obrigatória, avisa o PAR-Q, guarda rascunho e envia uma vez", async () => {
    const { repo, draft } = await renderWith(<ClientAnamnesisScreen />);

    expect(await screen.findByText("Qual é o seu principal objetivo?")).toBeOnTheScreen();
    await fireEvent.press(screen.getByRole("button", { name: "Continuar" }));
    expect(
      screen.getByText("Responda as perguntas obrigatórias para continuar."),
    ).toBeOnTheScreen();

    await fireEvent.press(screen.getByRole("radio", { name: "Ganhar massa muscular" }));
    await waitFor(async () => {
      expect(await draft.get()).toEqual({ goal: "hypertrophy" });
    });
    await fireEvent.press(screen.getByRole("button", { name: "Continuar" }));

    await fireEvent.press(
      screen.getByRole("button", { name: "Mais: Quantos dias por semana pode treinar?" }),
    );
    await fireEvent.press(screen.getByRole("button", { name: "Continuar" }));

    await fireEvent.press(screen.getByRole("radio", { name: "Sim" }));
    expect(screen.getByText(/recomendado é ter liberação médica/)).toBeOnTheScreen();
    await fireEvent.press(screen.getByRole("button", { name: "Continuar" }));

    await fireEvent.changeText(screen.getByLabelText("Remédios que usa"), "Losartana");
    await fireEvent.press(screen.getByRole("button", { name: "Enviar anamnese" }));

    expect(await screen.findByText("Anamnese enviada")).toBeOnTheScreen();
    expect(repo.submitted).toHaveLength(1);
    expect(repo.submitted[0]?.answers).toEqual({
      goal: "hypertrophy",
      weekly_days: 1,
      parq_heart: true,
      medications: "Losartana",
    });
    expect(await draft.get()).toBeNull();
  });

  it("aviso fixo na Home até enviar", async () => {
    await renderWith(<AnamnesisBanner />);
    expect(await screen.findByText("Complete sua anamnese")).toBeOnTheScreen();
  });

  it("aviso no exercício que envolve a região restrita", async () => {
    await renderWith(
      <>
        <RestrictionWarning linkId="link" muscle="quads" />
        <RestrictionWarning linkId="link" muscle="chest" />
      </>,
      createFakeAnamnesis([KNEE]),
    );
    expect(await screen.findByText("Atenção: Lesão · Joelho esq.")).toBeOnTheScreen();
    expect(screen.getAllByText(/Atenção:/)).toHaveLength(1);
  });
});
