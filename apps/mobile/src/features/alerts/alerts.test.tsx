import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react-native";
import type { ReactNode } from "react";
import { SafeAreaProvider } from "react-native-safe-area-context";
import { RepositoriesProvider, type Repositories } from "../../providers/repositories";
import { createFakeAlerts, createFakePushTokens } from "./data/fakes/fake-alerts";
import { byUrgency, clampThreshold, type Alert } from "./domain/alert";
import { AlertSettingsScreen } from "./ui/AlertSettingsScreen";
import { AlertsScreen } from "./ui/AlertsScreen";
import { PushRegistration } from "./ui/PushRegistration";

jest.mock("expo-router", () => ({ router: { back: jest.fn(), push: jest.fn() } }));
jest.setTimeout(20_000);

const METRICS = {
  frame: { x: 0, y: 0, width: 390, height: 844 },
  insets: { top: 0, left: 0, right: 0, bottom: 0 },
};

const HOUR = 3_600_000;

function alert(id: string, patch: Partial<Alert>): Alert {
  return {
    id,
    type: "new_feedback",
    severity: "info",
    status: "open",
    clientId: `client-${id}`,
    linkId: `link-${id}`,
    clientName: "Bia Lima",
    facts: {},
    createdAt: new Date(Date.now() - HOUR),
    snoozedUntil: null,
    ...patch,
  };
}

const SEED: readonly Alert[] = [
  alert("a1", { type: "inactive", severity: "warning", facts: { days: 8 }, clientName: "Caio" }),
  alert("a2", { type: "pain_reported", severity: "urgent", facts: { count: 1 } }),
  alert("a3", { type: "new_feedback", severity: "info", createdAt: new Date() }),
];

async function renderWith(
  ui: ReactNode,
  repo = createFakeAlerts(SEED),
  token: string | null = null,
) {
  const repositories = {
    alerts: repo,
    pushTokens: createFakePushTokens(token),
  } as unknown as Repositories;
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
  return repo;
}

describe("central de atenção", () => {
  it("do mais urgente para o menos; empate, o mais novo primeiro", () => {
    expect(byUrgency(SEED).map((a) => a.id)).toEqual(["a2", "a1", "a3"]);
    expect(clampThreshold("inactive", 1)).toBe(2);
    expect(clampThreshold("low_adherence", 140)).toBe(100);
  });

  it("lista com a dor no topo, resolve e adia pela folha", async () => {
    const repo = await renderWith(<AlertsScreen />);

    expect(await screen.findByRole("header", { name: "Urgente" })).toBeOnTheScreen();
    expect(screen.getByRole("tab", { name: "Abertos · 3" })).toBeOnTheScreen();
    expect(screen.getByText("Relatou dor no treino")).toBeOnTheScreen();
    expect(screen.getByText("8 dias sem treinar")).toBeOnTheScreen();

    await fireEvent.press(
      screen.getByRole("button", { name: "Resolver: Bia Lima, Relatou dor no treino" }),
    );
    await waitFor(() => {
      expect(screen.queryByText("Relatou dor no treino")).toBeNull();
    });

    await fireEvent.press(screen.getByRole("button", { name: "Adiar: Caio, 8 dias sem treinar" }));
    const sheet = await screen.findByRole("header", { name: "Adiar alerta" });
    expect(sheet).toBeOnTheScreen();
    await fireEvent.press(screen.getByRole("button", { name: "Em 3 dias" }));

    await waitFor(async () => {
      expect((await repo.list("snoozed", null)).items.map((a) => a.id)).toEqual(["a1"]);
    });
    expect((await repo.list("resolved", null)).items.map((a) => a.id)).toEqual(["a2"]);
  });

  it("sem alertas: estado vazio explica o que vai aparecer", async () => {
    await renderWith(<AlertsScreen />, createFakeAlerts([]));
    expect(await screen.findByText("Tudo em dia por aqui")).toBeOnTheScreen();
  });

  it("configura limite e push e salva tudo de uma vez", async () => {
    const repo = await renderWith(<AlertSettingsScreen />);

    expect(await screen.findByLabelText("dias sem treinar: 7")).toBeOnTheScreen();
    await fireEvent.press(screen.getByRole("button", { name: "Diminuir: Aluno sem treinar" }));
    await fireEvent.press(screen.getByRole("button", { name: "Diminuir: Aluno sem treinar" }));
    const [painPush] = screen.getAllByRole("switch", { name: "Avisar no celular" });
    if (painPush === undefined) {
      throw new Error("sem interruptor de push");
    }
    await fireEvent.press(painPush);
    await fireEvent.press(screen.getByRole("button", { name: "Salvar" }));

    await waitFor(async () => {
      const saved = await repo.settings();
      expect(saved.find((s) => s.type === "inactive")?.threshold).toBe(5);
      expect(saved.find((s) => s.type === "pain_reported")?.push).toBe(false);
    });
    expect(await screen.findByText("Alertas salvos.")).toBeOnTheScreen();
  });

  it("registra o aparelho para push ao abrir; sem token, segue sem push", async () => {
    const withToken = await renderWith(
      <PushRegistration />,
      createFakeAlerts(),
      "ExponentPushToken[abc12345]",
    );
    await waitFor(() => {
      expect(withToken.devices.has("ExponentPushToken[abc12345]")).toBe(true);
    });
    await screen.unmount();

    const without = await renderWith(<PushRegistration />, createFakeAlerts(), null);
    expect(without.devices.size).toBe(0);
  });
});
