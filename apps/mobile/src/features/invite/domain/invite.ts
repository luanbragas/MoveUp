// Convite do ponto de vista do aluno.

export interface InvitePreview {
  readonly professionalName: string;
  readonly organizationName: string;
  readonly expiresAt: Date;
}

export type MyLinkStatus = "pending" | "active" | "inactive";

export interface MyLink {
  readonly linkId: string;
  readonly status: MyLinkStatus;
  readonly startedAt: Date | null;
  readonly professionalName: string;
  readonly organizationName: string;
}

const CODE_ALPHABET = /^[ABCDEFGHJKMNPQRSTUVWXYZ23456789]{8}$/;

/** Código digitado ou vindo do link: maiúsculas, sem espaços; inválido = null. */
export function normalizeInviteCode(typed: string): string | null {
  const code = typed.replace(/\s+/g, "").toUpperCase();
  return CODE_ALPHABET.test(code) ? code : null;
}

/** Vínculo que vale para a casa do aluno: o ativo, se houver; senão o mais recente. */
export function currentLink(links: readonly MyLink[]): MyLink | null {
  return links.find((link) => link.status === "active") ?? links[0] ?? null;
}
