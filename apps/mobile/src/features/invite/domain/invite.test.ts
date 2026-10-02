import { currentLink, normalizeInviteCode, type MyLink } from "./invite";

const link = (status: MyLink["status"], id: string): MyLink => ({
  linkId: id,
  status,
  startedAt: null,
  professionalName: "Ana",
  organizationName: "Studio",
});

describe("convite do aluno", () => {
  it("normaliza o código digitado e recusa o inválido", () => {
    expect(normalizeInviteCode(" k7m2 qx9p ")).toBe("K7M2QX9P");
    expect(normalizeInviteCode("K7M2QX9")).toBeNull();
    expect(normalizeInviteCode("K7M2QX9O")).toBeNull(); // O não existe no alfabeto
  });

  it("vínculo atual é o ativo; senão o mais recente; sem vínculo, nenhum", () => {
    expect(currentLink([link("pending", "a"), link("active", "b")])?.linkId).toBe("b");
    expect(currentLink([link("inactive", "a"), link("pending", "b")])?.linkId).toBe("a");
    expect(currentLink([])).toBeNull();
  });
});
