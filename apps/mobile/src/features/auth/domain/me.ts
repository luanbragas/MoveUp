// Conta do usuário logado, no formato do domínio do app (não o DTO da API).
export type UserId = string & { readonly __brand: "UserId" };

export type WeightUnit = "kg" | "lb";
export type LengthUnit = "cm" | "in";

export interface Me {
  readonly id: UserId;
  readonly name: string;
  readonly email: string;
  readonly locale: string;
  readonly timezone: string;
  readonly units: { readonly weight: WeightUnit; readonly length: LengthUnit };
  readonly isProfessional: boolean;
}
