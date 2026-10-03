import type { ClientPage, Invitation, LinkId, NewClientInput, Seats } from "./client";

/** Alunos e vínculos do profissional (backend: módulo coaching). */
export interface ClientsRepository {
  list(cursor: string | null): Promise<ClientPage>;
  seats(): Promise<Seats>;
  invite(input: NewClientInput): Promise<Invitation>;
  resendInvite(linkId: LinkId): Promise<Invitation>;
  cancelInvite(linkId: LinkId): Promise<void>;
  inactivate(linkId: LinkId): Promise<void>;
  reactivate(linkId: LinkId): Promise<void>;
  end(linkId: LinkId): Promise<void>;
}
