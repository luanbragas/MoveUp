// Conta do usuário logado, no formato do domínio do app (não o DTO da API).
export type UserId = string & { readonly __brand: "UserId" };

export type WeightUnit = "kg" | "lb";
export type LengthUnit = "cm" | "in";
export type AccountRole = "professional" | "client";
export type ConsentKind = "terms" | "privacy" | "health_data" | "photos";
export type GuardianRelationship = "mother" | "father" | "legal_guardian" | "other";

/** Pedido do menor ao responsável, que autoriza pelo link no celular dele. */
export interface GuardianRequest {
  readonly status: "pending" | "declined";
  readonly guardianName: string;
  readonly relationship: GuardianRelationship;
  readonly requestedAt: Date;
  /** Validade do último link; null se recusado. */
  readonly linkExpiresAt: Date | null;
}

/** O que falta para liberar o app (SCREEN-FLOWS 0.2 e 1.2). */
export interface Onboarding {
  readonly missingConsents: readonly ConsentKind[];
  /** Menor de 18 sem autorização do responsável (LGPD, art. 14). */
  readonly guardianConsentRequired: boolean;
  /** Pedido aguardando ou recusado; null se ainda não pediu (ou já autorizou). */
  readonly guardianRequest: GuardianRequest | null;
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

/** Onboarding fechado: aceites em dia e, se menor, autorização do responsável. */
export function isOnboardingComplete(me: Me): boolean {
  return me.onboarding.missingConsents.length === 0 && !me.onboarding.guardianConsentRequired;
}
