import type { AccountRole, ConsentKind, Me } from "./me";

/** Porta da conta do usuário: o hook não sabe se vem da API ou de um fake. */
export interface MeRepository {
  getMe(): Promise<Me>;
}

export interface RegisterInput {
  readonly role: AccountRole;
  readonly name: string;
  /** ISO AAAA-MM-DD; obrigatória para aluno. */
  readonly birthDate: string | null;
  readonly businessName: string | null;
  readonly registryNumber: string | null;
}

export interface LegalVersions {
  readonly consents: Readonly<Record<ConsentKind, string>>;
  readonly guardianConsent: string;
}

export type GuardianRelationship = "mother" | "father" | "legal_guardian" | "other";

export interface GuardianInput {
  readonly guardianName: string;
  readonly guardianEmail: string;
  readonly relationship: GuardianRelationship;
}

/** Cadastro e consentimentos (backend: módulo accounts). */
export interface AccountRepository {
  register(input: RegisterInput): Promise<Me>;
  legalVersions(): Promise<LegalVersions>;
  /** Aceita os tipos informados na versão vigente. */
  grantConsents(kinds: readonly ConsentKind[], versions: LegalVersions): Promise<void>;
  declareGuardian(input: GuardianInput, versions: LegalVersions): Promise<void>;
}
