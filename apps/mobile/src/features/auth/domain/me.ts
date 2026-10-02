// Conta do usuário logado, no formato do domínio do app (não o DTO da API).
export type UserId = string & { readonly __brand: "UserId" };

export type WeightUnit = "kg" | "lb";
export type LengthUnit = "cm" | "in";
export type AccountRole = "professional" | "client";
export type ConsentKind = "terms" | "privacy" | "health_data" | "photos";

/** O que falta para liberar o app (SCREEN-FLOWS 0.2 e 1.2). */
export interface Onboarding {
  readonly missingConsents: readonly ConsentKind[];
  /** Menor de 18 sem consentimento do responsável (LGPD, art. 14). */
  readonly guardianConsentRequired: boolean;
}

export interface Me {
  readonly id: UserId;
  readonly name: string;
  readonly email: string;
  readonly locale: string;
  readonly timezone: string;
  readonly units: { readonly weight: WeightUnit; readonly length: LengthUnit };
  /** null: cadastro ainda sem papel escolhido. */
  readonly role: AccountRole | null;
  readonly isMinor: boolean;
  readonly onboarding: Onboarding;
}

/** Onboarding fechado: aceites em dia e, se menor, responsável registrado. */
export function isOnboardingComplete(me: Me): boolean {
  return me.onboarding.missingConsents.length === 0 && !me.onboarding.guardianConsentRequired;
}
