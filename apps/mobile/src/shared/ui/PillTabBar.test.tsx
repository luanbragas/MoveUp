import { fireEvent, render, screen } from "@testing-library/react-native";
import type { ComponentProps } from "react";
import { SafeAreaProvider } from "react-native-safe-area-context";
import { PillTabBar } from "./PillTabBar";

type Props = ComponentProps<typeof PillTabBar>;

function setup(index: number) {
  const navigate = jest.fn();
  const emit = jest.fn(() => ({ defaultPrevented: false }));
  const routes = [
    { key: "home-1", name: "home", params: undefined },
    { key: "history-1", name: "history", params: undefined },
  ];
  const props = {
    state: { index, routes },
    descriptors: {
      "home-1": { options: { title: "Hoje" } },
      "history-1": { options: { title: "Histórico" } },
    },
    navigation: { navigate, emit },
    insets: { top: 0, bottom: 0, left: 0, right: 0 },
    icons: { home: "home", history: "clock" },
  } as unknown as Props;
  return { props, navigate, emit };
}

const metrics = {
  frame: { x: 0, y: 0, width: 390, height: 844 },
  insets: { top: 0, left: 0, right: 0, bottom: 0 },
};

describe("barra de abas", () => {
  it("anuncia as abas com o nome e marca a atual", async () => {
    const { props } = setup(0);
    await render(
      <SafeAreaProvider initialMetrics={metrics}>
        <PillTabBar {...props} />
      </SafeAreaProvider>,
    );

    expect(screen.getByRole("tab", { name: "Hoje" })).toBeSelected();
    expect(screen.getByRole("tab", { name: "Histórico" })).not.toBeSelected();
    expect(screen.getByText("Hoje")).toBeOnTheScreen(); // só a ativa mostra o rótulo
    expect(screen.queryByText("Histórico")).toBeNull();
  });

  it("toque em outra aba navega; na aba atual, não", async () => {
    const { props, navigate, emit } = setup(0);
    await render(
      <SafeAreaProvider initialMetrics={metrics}>
        <PillTabBar {...props} />
      </SafeAreaProvider>,
    );

    await fireEvent.press(screen.getByRole("tab", { name: "Histórico" }));
    await fireEvent.press(screen.getByRole("tab", { name: "Hoje" }));

    expect(emit).toHaveBeenCalledTimes(2);
    expect(navigate).toHaveBeenCalledTimes(1);
    expect(navigate).toHaveBeenCalledWith("history", undefined);
  });
});
