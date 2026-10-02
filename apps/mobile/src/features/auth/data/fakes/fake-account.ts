import { ApiFailure } from "../../../../shared/lib/http";
import type { AuthSession } from "../../domain/session";
import type { ConsentKind, Me, UserId } from "../../domain/me";
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

/**
 * Backend em memória para testes de hook: imita as regras de onboarding da API
 * (aceites por papel, responsável para menor de 18).
 */
export function createFakeAccount(session: AuthSession): MeRepository & AccountRepository {
  const accounts = new Map<
    string,
    { me: Me; accepted: Set<ConsentKind>; guardian: boolean; birthDate: string | null }
  >();

  const currentUid = () => {
    const user = session.current();
    if (user === null) {
      throw new ApiFailure({ kind: "problem", code: "unauthenticated", status: 401, traceId: "t" });
    }
    return user.uid;
  };

  const snapshot = (uid: string): Me => {
    const account = accounts.get(uid);
    if (account === undefined) {
      throw notRegistered();
    }
    const role = account.me.role ?? "client";
    return {
      ...account.me,
      onboarding: {
        missingConsents: REQUIRED[role].filter((kind) => !account.accepted.has(kind)),
        guardianConsentRequired: account.me.isMinor && !account.guardian,
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
            onboarding: { missingConsents: [], guardianConsentRequired: false },
          },
          accepted: new Set(),
          guardian: false,
          birthDate: input.birthDate,
        });
        return snapshot(uid);
      }),
    legalVersions: () => Promise.resolve(FAKE_VERSIONS),
    grantConsents: (kinds) =>
      Promise.resolve().then(() => {
        const account = accounts.get(currentUid());
        if (account === undefined) {
          throw notRegistered();
        }
        kinds.forEach((kind) => account.accepted.add(kind));
      }),
    declareGuardian: () =>
      Promise.resolve().then(() => {
        const account = accounts.get(currentUid());
        if (account === undefined) {
          throw notRegistered();
        }
        account.guardian = true;
      }),
  };
}
