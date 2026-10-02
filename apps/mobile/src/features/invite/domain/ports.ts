import type { InvitePreview, MyLink } from "./invite";

/** Convite e vínculos do aluno (backend: módulo coaching). */
export interface InviteRepository {
  preview(code: string): Promise<InvitePreview>;
  /** @returns id do vínculo, agora ativo */
  accept(code: string): Promise<string>;
  myLinks(): Promise<readonly MyLink[]>;
  endMyLink(linkId: string): Promise<void>;
}
