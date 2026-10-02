// Alunos do profissional, no formato do domínio do app (não o DTO da API).

export type LinkId = string & { readonly __brand: "LinkId" };
export type LinkStatus = "pending" | "active" | "inactive";

export interface PendingInvite {
  readonly code: string;
  readonly expiresAt: Date;
}

export interface ClientItem {
  readonly linkId: LinkId;
  readonly clientId: string;
  readonly name: string;
  readonly status: LinkStatus;
  readonly startedAt: Date | null;
  readonly pendingInvite: PendingInvite | null;
}

export interface ClientPage {
  readonly items: readonly ClientItem[];
  readonly nextCursor: string | null;
}

/** Convite pronto para compartilhar (link, código ou QR). */
export interface Invitation {
  readonly linkId: LinkId;
  readonly code: string;
  readonly url: string;
  readonly expiresAt: Date;
}

export interface NewClientInput {
  readonly name: string;
  readonly email: string | null;
  /** WhatsApp com DDI e DDD; só dígitos ou formatado. */
  readonly phone: string | null;
  readonly goal: string | null;
}

/** Ações possíveis para um aluno, conforme o estado do vínculo (SCREEN-FLOWS 2.2). */
export type ClientAction =
  "share" | "resend" | "cancel-invite" | "inactivate" | "reactivate" | "end";

export function availableActions(client: ClientItem): readonly ClientAction[] {
  switch (client.status) {
    case "pending":
      return client.pendingInvite === null
        ? ["resend", "end"]
        : ["share", "resend", "cancel-invite", "end"];
    case "active":
      return ["inactivate", "end"];
    case "inactive":
      return ["reactivate", "end"];
  }
}

/** Texto do convite para WhatsApp e compartilhamento (sem dado de saúde). */
export function inviteMessage(
  professionalFirstName: string | null,
  invitation: Pick<Invitation, "code" | "url">,
): string {
  const who = professionalFirstName ?? "Seu personal";
  return `${who} te convidou para o MoveUp. Baixe o app e use o código ${invitation.code} ou abra: ${invitation.url}`;
}

/** Link do WhatsApp para o número do aluno (só dígitos, com DDI). */
export function whatsappLink(phone: string, message: string): string | null {
  const digits = phone.replace(/\D/g, "");
  if (digits.length < 10 || digits.length > 15) {
    return null;
  }
  return `https://wa.me/${digits}?text=${encodeURIComponent(message)}`;
}

/**
 * Base do link do convite; igual a `moveup.invite.link-base-url` do backend (provisória até o
 * site do convite, F1-6). A lista de alunos traz só o código, então o link é montado aqui.
 */
const INVITE_BASE_URL = "https://moveup.com.br/i/";

export function inviteUrl(code: string): string {
  return `${INVITE_BASE_URL}${code}`;
}

/** Primeiro nome para a mensagem do convite (nome vazio = sem nome). */
export function firstName(fullName: string): string | null {
  const first = fullName.trim().split(/\s+/)[0];
  return first === undefined || first === "" ? null : first;
}
