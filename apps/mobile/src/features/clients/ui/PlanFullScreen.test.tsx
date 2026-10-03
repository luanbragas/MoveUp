import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react-native";
import { Alert } from "react-native";
import { RepositoriesProvider, type Repositories } from "../../../providers/repositories";
import { isFull, type ClientItem, type LinkId } from "../domain/client";
import { PlanFullScreen } from "./PlanFullScreen";

jest.mock("expo-router", () => ({ router: { back: jest.fn() } }));

const active = (n: number, name: string): ClientItem => ({
  linkId: `link-${String(n)}` as LinkId,
  clientId: `client-${String(n)}`,
  name,
  status: "active",
  startedAt: new Date("2026-09-01"),
  pendingInvite: null,
});

describe("plano lotado", () => {
  it("vaga cheia: limite atingido ou sem assinatura", () => {
    expect(isFull({ active: 10, limit: 10 })).toBe(true);
    expect(isFull({ active: 3, limit: null })).toBe(true);
    expect(isFull({ active: 9, limit: 10 })).toBe(false);
  });

  it("escolhe um aluno ativo e libera a vaga dele depois de confirmar", async () => {
    const inactivate = jest.fn(() => Promise.resolve());
    const unused = () => Promise.reject(new Error("fora deste teste"));
    const repositories = {
      clients: {
        list: () =>
          Promise.resolve({
            items: [active(1, "Bia Lima"), active(2, "Carlos Menezes")],
            nextCursor: null,
          }),
        seats: () => Promise.resolve({ active: 2, limit: 2 }),
        invite: unused,
        resendInvite: unused,
        cancelInvite: unused,
        inactivate,
        reactivate: unused,
        end: unused,
      },
    } as unknown as Repositories;
    const onReleased = jest.fn();
    jest.spyOn(Alert, "alert").mockImplementation((_title, _message, buttons) => {
      buttons?.find((button) => button.style === "destructive")?.onPress?.();
    });
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false, gcTime: Number.POSITIVE_INFINITY },
        mutations: { gcTime: Number.POSITIVE_INFINITY },
      },
    });

    await render(
      <QueryClientProvider client={queryClient}>
        <RepositoriesProvider repositories={repositories}>
          <PlanFullScreen seats={{ active: 2, limit: 2 }} onReleased={onReleased} />
        </RepositoriesProvider>
      </QueryClientProvider>,
    );

    expect(screen.getByText(/Suas 2 vagas estão ocupadas/)).toBeOnTheScreen();
    expect(screen.getByRole("button", { name: "Toque em um aluno acima" })).toBeDisabled();

    await fireEvent.press(await screen.findByRole("radio", { name: "Carlos Menezes" }));
    await fireEvent.press(screen.getByRole("button", { name: "Liberar a vaga de Carlos" }));

    await waitFor(() => {
      expect(onReleased).toHaveBeenCalled();
    });
    expect(inactivate).toHaveBeenCalledWith("link-2");
  });
});
