import { render, screen } from "@testing-library/react-native";
import { BodyMap } from "./BodyMap";

describe("BodyMap", () => {
  it("desenha o lado certo e descreve os músculos para o leitor de tela", async () => {
    await render(<BodyMap levels={{ lats: 2, biceps: 1 }} width={200} height={200} />);
    expect(
      screen.getByRole("image", { name: "Costas; principal: dorsal; secundário: bíceps" }),
    ).toBeOnTheScreen();
  });

  it("aceita o corpo inteiro sem zoom", async () => {
    await render(
      <BodyMap levels={{ quads: 2 }} width={120} height={230} view="front" zoom={false} />,
    );
    expect(
      screen.getByRole("image", { name: "Frente do corpo; principal: quadríceps" }),
    ).toBeOnTheScreen();
  });
});
