import { onlineManager } from "@tanstack/react-query";
import { act, fireEvent, render, screen } from "@testing-library/react-native";
import { SafeAreaProvider } from "react-native-safe-area-context";
import { Button } from "./Button";
import { Checkbox } from "./Checkbox";
import { EmptyState } from "./EmptyState";
import { ErrorScreen } from "./ErrorScreen";
import { Screen } from "./Screen";
import { ChoiceGroup } from "./ChoiceGroup";
import { Segmented } from "./Segmented";
import { TextField } from "./TextField";
import { Toggle } from "./Toggle";

const METRICS = {
  frame: { x: 0, y: 0, width: 390, height: 844 },
  insets: { top: 0, left: 0, right: 0, bottom: 0 },
};

// Componentes base do visual "Impacto": o que importa é o comportamento e o que o leitor de tela recebe.

describe("componentes base", () => {
  it("botão chama a ação e, desabilitado ou carregando, não chama", async () => {
    const onPress = jest.fn();
    await render(<Button label="Começar treino" onPress={onPress} />);
    await fireEvent.press(screen.getByRole("button", { name: "Começar treino" }));
    expect(onPress).toHaveBeenCalledTimes(1);

    await render(<Button label="Salvar" onPress={onPress} disabled />);
    const disabled = screen.getByRole("button", { name: "Salvar" });
    expect(disabled).toBeDisabled();

    await render(<Button label="Enviar" onPress={onPress} loading />);
    expect(screen.getByRole("button", { name: "Enviar" })).toBeBusy();
  });

  it("interruptor anuncia o estado e alterna", async () => {
    const onChange = jest.fn();
    await render(<Toggle label="Fotos de evolução" value={false} onChange={onChange} />);
    const toggle = screen.getByRole("switch", { name: "Fotos de evolução" });
    expect(toggle).not.toBeChecked();
    await fireEvent.press(toggle);
    expect(onChange).toHaveBeenCalledWith(true);
  });

  it("consentimento começa desmarcado e marca ao tocar", async () => {
    const onChange = jest.fn();
    await render(<Checkbox label="Dados de saúde" checked={false} onChange={onChange} />);
    const box = screen.getByRole("checkbox", { name: "Dados de saúde" });
    expect(box).not.toBeChecked();
    await fireEvent.press(box);
    expect(onChange).toHaveBeenCalledWith(true);
  });

  it("seletor e grupo de opções marcam a escolha", async () => {
    const onTab = jest.fn();
    await render(
      <Segmented
        label="Evolução"
        value="weight"
        onChange={onTab}
        options={[
          { value: "weight", label: "Peso" },
          { value: "photos", label: "Fotos" },
        ]}
      />,
    );
    expect(screen.getByRole("tab", { name: "Peso" })).toBeSelected();
    await fireEvent.press(screen.getByRole("tab", { name: "Fotos" }));
    expect(onTab).toHaveBeenCalledWith("photos");

    const onPick = jest.fn();
    await render(
      <ChoiceGroup
        label="Parentesco"
        value="mother"
        onChange={onPick}
        options={[
          { value: "mother", label: "Mãe" },
          { value: "father", label: "Pai" },
        ]}
      />,
    );
    expect(screen.getByRole("radio", { name: "Mãe" })).toBeChecked();
    await fireEvent.press(screen.getByRole("radio", { name: "Pai" }));
    expect(onPick).toHaveBeenCalledWith("father");
  });

  it("campo com erro mostra a mensagem e a usa como dica do leitor de tela", async () => {
    await render(
      <TextField label="E-mail" value="ana@" onChangeText={jest.fn()} error="Confira o e-mail." />,
    );
    expect(screen.getByText("Confira o e-mail.")).toBeOnTheScreen();
    expect(screen.getByLabelText("E-mail")).toHaveProp("accessibilityHint", "Confira o e-mail.");
  });
});

describe("estados de tela", () => {
  afterEach(async () => {
    await act(() => {
      onlineManager.setOnline(true);
    });
  });

  it("vazio mostra o que vai aparecer e o próximo passo", async () => {
    const onPress = jest.fn();
    await render(
      <EmptyState
        icon="users"
        title="Convide seu primeiro aluno"
        text="Pelo WhatsApp ou QR Code."
        action={<Button label="Convidar aluno" onPress={onPress} />}
      />,
    );
    expect(screen.getByRole("header", { name: "Convide seu primeiro aluno" })).toBeOnTheScreen();
    await fireEvent.press(screen.getByRole("button", { name: "Convidar aluno" }));
    expect(onPress).toHaveBeenCalled();
  });

  it("erro inesperado tenta de novo e mostra o código só quando existe", async () => {
    const onRetry = jest.fn();
    await render(
      <SafeAreaProvider initialMetrics={METRICS}>
        <ErrorScreen onRetry={onRetry} code="trace-123" />
      </SafeAreaProvider>,
    );
    expect(screen.getByText(/trace-123/)).toBeOnTheScreen();
    await fireEvent.press(screen.getByRole("button", { name: "Tentar de novo" }));
    expect(onRetry).toHaveBeenCalled();
  });

  it("sem internet, toda tela avisa no topo", async () => {
    onlineManager.setOnline(false);
    await render(
      <SafeAreaProvider initialMetrics={METRICS}>
        <Screen title="Alunos" />
      </SafeAreaProvider>,
    );
    expect(screen.getByText(/Sem internet/)).toBeOnTheScreen();
  });
});
