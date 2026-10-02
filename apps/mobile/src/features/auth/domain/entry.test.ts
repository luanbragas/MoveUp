import { entryDestination } from "./entry";
import type { Me, UserId } from "./me";

function me(overrides: Partial<Me> = {}): Me {
  return {
    id: "0192f5c4-1b2a-7c3d-8e4f-5a6b7c8d9e0f" as UserId,
    name: "Fictícia",
    email: "f@example.test",
    locale: "pt-BR",
    timezone: "America/Sao_Paulo",
    units: { weight: "kg", length: "cm" },
    role: "client",
    isMinor: false,
    onboarding: { missingConsents: [], guardianConsentRequired: false },
    ...overrides,
  };
}

describe("entryDestination", () => {
  it("espera a sessão e a conta carregarem", () => {
    expect(entryDestination(null, { kind: "loading" })).toBe("loading");
    expect(entryDestination(true, { kind: "loading" })).toBe("loading");
  });

  it("sem sessão vai para as boas-vindas", () => {
    expect(entryDestination(false, { kind: "loading" })).toBe("welcome");
  });

  it("login sem conta vai para o cadastro; erro mostra tela de erro", () => {
    expect(entryDestination(true, { kind: "not-registered" })).toBe("register");
    expect(entryDestination(true, { kind: "failed" })).toBe("error");
    expect(entryDestination(true, { kind: "found", me: me({ role: null }) })).toBe("register");
  });

  it("consentimentos pendentes vêm antes do responsável", () => {
    const pending = me({
      isMinor: true,
      onboarding: { missingConsents: ["terms"], guardianConsentRequired: true },
    });
    const guardianOnly = me({
      isMinor: true,
      onboarding: { missingConsents: [], guardianConsentRequired: true },
    });

    expect(entryDestination(true, { kind: "found", me: pending })).toBe("consents");
    expect(entryDestination(true, { kind: "found", me: guardianOnly })).toBe("guardian");
  });

  it("onboarding completo leva à casa do papel", () => {
    expect(entryDestination(true, { kind: "found", me: me({ role: "professional" }) })).toBe(
      "professional-home",
    );
    expect(entryDestination(true, { kind: "found", me: me() })).toBe("client-home");
  });
});
