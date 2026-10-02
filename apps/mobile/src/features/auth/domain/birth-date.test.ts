import { parseBirthDate } from "./birth-date";

describe("parseBirthDate", () => {
  it("converte DD/MM/AAAA para ISO", () => {
    expect(parseBirthDate("07/03/2011")).toBe("2011-03-07");
    expect(parseBirthDate(" 29/02/2008 ")).toBe("2008-02-29");
  });

  it("recusa formato errado e data que não existe", () => {
    expect(parseBirthDate("7/3/2011")).toBeNull();
    expect(parseBirthDate("2011-03-07")).toBeNull();
    expect(parseBirthDate("31/02/2011")).toBeNull();
    expect(parseBirthDate("29/02/2011")).toBeNull();
    expect(parseBirthDate("00/01/2011")).toBeNull();
  });
});
