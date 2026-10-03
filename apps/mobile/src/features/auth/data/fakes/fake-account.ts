import { ApiFailure } from "../../../../shared/lib/http";
import type { AuthSession } from "../../domain/session";
import type { ConsentKind, GuardianRequest, Me, UserId } from "../../domain/me";
import type { AccountRepository, LegalVersions, MeRepository } from "../../domain/ports";

const REQUIRED: Record<"professional" | "client", readonly ConsentKind[]> = {
  professional: ["privacy", "terms"],
  client: ["health_data", "privacy", "terms"],
};

export const FAKE_VERSIONS: LegalVersions = {
  consents: { terms: "v1", privacy: "v1", health_data: "v1", photos: "v1" },
  guardianConsent: "v1",
};

const notRegistered = () =>
  new ApiFailure({ kind: "problem", code: "account-not-registered", status: 404, traceId: "t" });

type GuardianState =
  | { readonly status: "none" | "verified" }
  | { readonly status: "pending" | "declined"; readonly request: GuardianRequest };

/** O que o responsável faz na página do link (fora do app), para os testes simularem. */
export interface FakeGuardianControls {
  guardianDecides(uid: string, approve: boolean): void;
}

const LINK_TTL_MS = 7 * 24 * 60 * 60 * 1000;

/**
 * Backend em memória para testes de hook: imita as regras de onboarding da API
 * (aceites por papel; menor de 18 precisa que o responsável autorize pelo link).
 */
export function createFakeAccount(
  session: AuthSession,
): MeRepository & AccountRepository & FakeGuardianControls {
  const accounts = new Map<
    string,
    { me: Me; accepted: Set<ConsentKind>; guardian: GuardianState; birthDate: string | null }
  >();
  let links = 0;

  const accountOf = (uid: string) => {
    const account = accounts.get(uid);
    if (account === undefined) {
      throw notRegistered();
    }
    return account;
  };

  const conflict = (code: string) =>
    new ApiFailure({ kind: "problem", code, status: 409, traceId: "t" });

  const newLink = () => {
    links += 1;
    return {
      url: "https://site.test/autorizar/#segredo-" + String(links),
      expiresAt: new Date(Date.now() + LINK_TTL_MS),
    };
  };

  const currentUid = () => {
    const user = session.current();
    if (user === null) {
      throw new ApiFailure({ kind: "problem", code: "unauthenticated", status: 401, traceId: "t" });
    }
    return user.uid;
  };

  const snapshot = (uid: string): Me => {
    const account = accountOf(uid);
    const role = account.me.role ?? "client";
    return {
      ...account.me,
      onboarding: {
        missingConsents: REQUIRED[role].filter((kind) => !account.accepted.has(kind)),
        guardianConsentRequired: account.me.isMinor && account.guardian.status !== "verified",
        guardianRequest:
          account.guardian.status === "pending" || account.guardian.status === "declined"
            ? account.guardian.request
            : null,
      },
    };
  };

  return {
    getMe: () => Promise.resolve().then(() => snapshot(currentUid())),
    register: (input) =>
      Promise.resolve().then(() => {
        const uid = currentUid();
        const age =
          input.birthDate === null
            ? 99
            : new Date().getUTCFullYear() - Number(input.birthDate.slice(0, 4));
        accounts.set(uid, {
          me: {
            id: `id-${uid}` as UserId,
            name: input.name,
            email: session.current()?.email ?? "",
            locale: "pt-BR",
            timezone: "America/Sao_Paulo",
            units: { weight: "kg", length: "cm" },
            role: input.role,
            isMinor: age < 18,
            onboarding: {
              missingConsents: [],
              guardianConsentRequired: false,
              guardianRequest: null,
            },
          },
          accepted: new Set(),
          guardian: { status: "none" },
          birthDate: input.birthDate,
        });
        return snapshot(uid);
      }),
    legalVersions: () => Promise.resolve(FAKE_VERSIONS),
    grantConsents: (kinds) =>
      Promise.resolve().then(() => {
        const account = accountOf(currentUid());
        kinds.forEach((kind) => account.accepted.add(kind));
      }),
    requestGuardian: (input) =>
      Promise.resolve().then(() => {
        const account = accountOf(currentUid());
        if (!account.me.isMinor) {
          throw conflict("guardian-consent-not-required");
        }
        if (account.guardian.status === "pending" || account.guardian.status === "verified") {
          throw conflict("guardian-consent-already-active");
        }
        const link = newLink();
        account.guardian = {
          status: "pending",
          request: {
            status: "pending",
            guardianName: input.guardianName,
            relationship: input.relationship,
            requestedAt: new Date(),
            linkExpiresAt: link.expiresAt,
          },
        };
        return link;
      }),
    resendGuardianLink: () =>
      Promise.resolve().then(() => {
        const account = accountOf(currentUid());
        if (account.guardian.status !== "pending") {
          throw conflict("guardian-request-not-pending");
        }
        const link = newLink();
        account.guardian = {
          status: "pending",
          request: { ...account.guardian.request, linkExpiresAt: link.expiresAt },
        };
        return link;
      }),
    cancelGuardianRequest: () =>
      Promise.resolve().then(() => {
        const account = accountOf(currentUid());
        if (account.guardian.status !== "pending") {
          throw conflict("guardian-request-not-pending");
        }
        account.guardian = { status: "none" };
      }),
    guardianDecides: (uid, approve) => {
      const account = accountOf(uid);
      if (account.guardian.status !== "pending") {
        throw new Error("sem pedido aguardando o responsável");
      }
      account.guardian = approve
        ? { status: "verified" }
        : {
            status: "declined",
            request: { ...account.guardian.request, status: "declined", linkExpiresAt: null },
          };
    },
  };
}
