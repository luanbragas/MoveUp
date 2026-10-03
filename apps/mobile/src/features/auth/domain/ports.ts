import type { AccountRole, ConsentKind, GuardianRelationship, Me } from "./me";

export type { GuardianRelationship } from "./me";

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

export interface GuardianInput {
  readonly guardianName: string;
  readonly relationship: GuardianRelationship;
}

/** Link para o responsável. Só existe na resposta: o app compartilha na hora. */
export interface GuardianLink {
  readonly url: string;
  readonly expiresAt: Date;
}

/** Cadastro e consentimentos (backend: módulo accounts). */
export interface AccountRepository {
  register(input: RegisterInput): Promise<Me>;
  legalVersions(): Promise<LegalVersions>;
  /** Aceita os tipos informados na versão vigente. */
  grantConsents(kinds: readonly ConsentKind[], versions: LegalVersions): Promise<void>;
  /** Menor indica o responsável e recebe o link para mandar a essa pessoa. */
  requestGuardian(input: GuardianInput, versions: LegalVersions): Promise<GuardianLink>;
  /** Link novo para o mesmo pedido; o anterior deixa de valer. */
  resendGuardianLink(): Promise<GuardianLink>;
  /** Desiste do pedido para indicar outra pessoa. */
  cancelGuardianRequest(): Promise<void>;
}
