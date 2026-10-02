import { router } from "expo-router";
import { z } from "zod";
import type { Invitation } from "../domain/client";

// Parâmetros da rota /invite-share: chegam como texto da URL, então passam pelo Zod.
const ShareParamsSchema = z.object({
  code: z.string().min(1),
  url: z.url(),
  expiresAt: z.iso.datetime({ offset: true }),
  name: z.string().optional(),
  phone: z.string().optional(),
});

export interface ShareParams {
  readonly code: string;
  readonly url: string;
  readonly expiresAt: Date;
  readonly name: string | null;
  readonly phone: string | null;
}

export function parseShareParams(raw: unknown): ShareParams | null {
  const parsed = ShareParamsSchema.safeParse(raw);
  if (!parsed.success) {
    return null;
  }
  const { code, url, expiresAt, name, phone } = parsed.data;
  return {
    code,
    url,
    expiresAt: new Date(expiresAt),
    name: name === undefined || name === "" ? null : name,
    phone: phone === undefined || phone === "" ? null : phone,
  };
}

/** Abre a tela de compartilhar; `replace` quando vem do formulário (voltar não reabre o form). */
export function openShare(
  invitation: Pick<Invitation, "code" | "url" | "expiresAt">,
  who: { readonly name: string | null; readonly phone: string | null },
  mode: "push" | "replace",
): void {
  const params = {
    code: invitation.code,
    url: invitation.url,
    expiresAt: invitation.expiresAt.toISOString(),
    name: who.name ?? "",
    phone: who.phone ?? "",
  };
  const href = { pathname: "/invite-share", params } as const;
  if (mode === "replace") {
    router.replace(href);
  } else {
    router.push(href);
  }
}
