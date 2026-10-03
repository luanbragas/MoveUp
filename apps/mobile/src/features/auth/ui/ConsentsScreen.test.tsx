import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react-native";
import { router } from "expo-router";
import { createFakeAnamnesis } from "../../anamnesis/data/fakes/fake-anamnesis";
import { createMemoryAnamnesisDraft } from "../../anamnesis/data/draft-store";
import { createFakeAlerts, createFakePushTokens } from "../../alerts/data/fakes/fake-alerts";
import { createMemoryDraftStore } from "../../training/data/draft-store";
import { RepositoriesProvider, type Repositories } from "../../../providers/repositories";
import { createMemorySessionStore } from "../../execution/data/session-stores";
import { createMemoryStore } from "../../sync/data/memory-store";
import { createFakeAccount } from "../data/fakes/fake-account";
import { createFakeSession } from "../data/fakes/fake-session";
import { ConsentsScreen } from "./ConsentsScreen";

jest.mock("expo-router", () => ({
  router: { replace: jest.fn(), back: jest.fn(), canGoBack: () => false },
  Redirect: () => null,
}));

async function renderAs(role: "client" | "professional") {
  const session = createFakeSession({ uid: "uid-bia", email: "bia@example.test" });
  const fake = createFakeAccount(session);
  await fake.register({
    role,
    name: "Bia Souza",
    birthDate: "1990-01-01",
    businessName: null,
    registryNumber: null,
  });
  const unused = () => Promise.reject(new Error("fora deste teste"));
  const repositories: Repositories = {
    session,
    me: fake,
    account: fake,
    clients: {
      list: unused,
      seats: unused,
      invite: unused,
      resendInvite: unused,
      cancelInvite: unused,
      inactivate: unused,
      reactivate: unused,
      end: unused,
    },
    invite: { preview: unused, accept: unused, myLinks: unused, endMyLink: unused },
    exercises: { search: unused, create: unused, archive: unused },
    training: {
      listTemplates: unused,
      createTemplate: unused,
      getWorkout: unused,
      saveWorkout: unused,
      archiveWorkout: unused,
      activeProgram: unused,
      createProgram: unused,
      updateProgram: unused,
      addProgramWorkout: unused,
    },
    syncApi: { changesSince: unused },
    plannedStore: createMemoryStore(),
    sessionStore: createMemorySessionStore(),
    sessionSyncApi: { push: unused },
    restAlarm: { schedule: () => Promise.resolve(null), cancel: () => Promise.resolve() },
    workoutDrafts: createMemoryDraftStore(),
    alerts: createFakeAlerts(),
    pushTokens: createFakePushTokens(),
    anamnesis: createFakeAnamnesis(),
    anamnesisDraft: createMemoryAnamnesisDraft(),
  };
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: Number.POSITIVE_INFINITY },
      mutations: { gcTime: Number.POSITIVE_INFINITY },
    },
  });
  await render(
    <QueryClientProvider client={queryClient}>
      <RepositoriesProvider repositories={repositories}>
        <ConsentsScreen />
      </RepositoriesProvider>
    </QueryClientProvider>,
  );
  return fake;
}

describe("termos e privacidade", () => {
  it("aluno: tudo começa desligado e só segue com termos, privacidade e saúde", async () => {
    const fake = await renderAs("client");
    const submit = await screen.findByRole("button", { name: "Aceitar e continuar" });

    for (const label of ["Termos de uso", "Privacidade", "Dados de saúde", "Fotos de evolução"]) {
      expect(screen.getByRole("switch", { name: label })).not.toBeChecked();
    }
    expect(submit).toBeDisabled();

    await fireEvent.press(screen.getByRole("switch", { name: "Termos de uso" }));
    await fireEvent.press(screen.getByRole("switch", { name: "Privacidade" }));
    expect(screen.getByText("Falta aceitar: dados de saúde")).toBeOnTheScreen();
    expect(submit).toBeDisabled();

    await fireEvent.press(screen.getByRole("switch", { name: "Dados de saúde" }));
    expect(submit).toBeEnabled(); // fotos são opcionais
    await fireEvent.press(submit);

    await waitFor(() => {
      expect(router.replace).toHaveBeenCalledWith("/");
    });
    expect((await fake.getMe()).onboarding.missingConsents).toEqual([]);
  });

  it("personal não vê dados de saúde nem fotos e termina na tela de pronto", async () => {
    await renderAs("professional");
    await screen.findByRole("switch", { name: "Termos de uso" });

    expect(screen.queryByRole("switch", { name: "Dados de saúde" })).toBeNull();
    expect(screen.queryByRole("switch", { name: "Fotos de evolução" })).toBeNull();

    await fireEvent.press(screen.getByRole("switch", { name: "Termos de uso" }));
    await fireEvent.press(screen.getByRole("switch", { name: "Privacidade" }));
    await fireEvent.press(screen.getByRole("button", { name: "Aceitar e continuar" }));

    await waitFor(() => {
      expect(router.replace).toHaveBeenCalledWith("/ready");
    });
  });
});
