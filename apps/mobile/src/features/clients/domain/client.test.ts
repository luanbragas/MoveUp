import {
  availableActions,
  firstName,
  inviteMessage,
  inviteUrl,
  whatsappLink,
  type ClientItem,
  type LinkId,
} from "./client";

const client = (overrides: Partial<ClientItem>): ClientItem => ({
  linkId: "link" as LinkId,
  clientId: "client",
  name: "Bia",
  status: "pending",
  startedAt: null,
  pendingInvite: null,
  ...overrides,
});

describe("alunos do profissional", () => {
  it("ações dependem do estado do vínculo", () => {
    expect(
      availableActions(client({ pendingInvite: { code: "K7M2QX9P", expiresAt: new Date() } })),
    ).toEqual(["share", "resend", "cancel-invite", "end"]);
    expect(availableActions(client({}))).toEqual(["resend", "end"]);
    expect(availableActions(client({ status: "active" }))).toEqual(["inactivate", "end"]);
    expect(availableActions(client({ status: "inactive" }))).toEqual(["reactivate", "end"]);
  });

  it("mensagem e link do WhatsApp com o código e o link do convite", () => {
    const invitation = {
      linkId: "l" as LinkId,
      code: "K7M2QX9P",
      url: "https://moveup.com.br/i/K7M2QX9P",
      expiresAt: new Date(),
    };
    const message = inviteMessage("Ana", invitation);

    expect(message).toContain("K7M2QX9P");
    expect(message).toContain("https://moveup.com.br/i/K7M2QX9P");
    expect(whatsappLink("+55 (11) 98765-4321", message)).toBe(
      `https://wa.me/5511987654321?text=${encodeURIComponent(message)}`,
    );
    expect(whatsappLink("123", message)).toBeNull();
  });
});

describe("link e primeiro nome", () => {
  it("monta o link do convite e pega o primeiro nome", () => {
    expect(inviteUrl("K7M2QX9P")).toBe("https://moveup.com.br/i/K7M2QX9P");
    expect(firstName("  Ana  Souza ")).toBe("Ana");
  });
});
