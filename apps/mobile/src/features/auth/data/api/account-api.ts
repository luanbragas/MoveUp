import {
  declareGuardianConsent,
  getLegalVersions,
  grantConsents,
  registerAccount,
  type RegisterAccount,
  schemas,
} from "@moveup/api-client";
import { z } from "zod";
import { callApi } from "../../../../shared/lib/http";
import type { AccountRepository, LegalVersions } from "../../domain/ports";
import { toMe } from "./me-api";

const NoContent = z.undefined();

/** Texto vazio vira ausência. */
function optional(value: string | null): string | undefined {
  return value === null || value.trim() === "" ? undefined : value.trim();
}

export function createAccountApiRepository(): AccountRepository {
  return {
    async register(input) {
      // Só os campos preenchidos: a API espera ausência, não string vazia.
      const body: RegisterAccount = { name: input.name.trim(), role: input.role };
      const businessName = optional(input.businessName);
      const registryNumber = optional(input.registryNumber);
      if (input.birthDate !== null) {
        body.birthDate = input.birthDate;
      }
      if (businessName !== undefined) {
        body.businessName = businessName;
      }
      if (registryNumber !== undefined) {
        body.registryNumber = registryNumber;
      }
      const dto = await callApi(() => registerAccount(body), schemas.RegisterAccount201Response);
      return toMe(dto);
    },

    async legalVersions(): Promise<LegalVersions> {
      const dto = await callApi(() => getLegalVersions(), schemas.GetLegalVersions200Response);
      const version = (kind: string): string => {
        const value = dto.consents[kind];
        if (value === undefined) {
          throw new Error(`versão ausente para ${kind}`); // bug de contrato
        }
        return value;
      };
      return {
        consents: {
          terms: version("terms"),
          privacy: version("privacy"),
          health_data: version("health_data"),
          photos: version("photos"),
        },
        guardianConsent: dto.guardianConsent,
      };
    },

    async grantConsents(kinds, versions) {
      await callApi(
        () =>
          grantConsents({
            grants: kinds.map((kind) => ({ kind, docVersion: versions.consents[kind] })),
          }),
        NoContent,
      );
    },

    async declareGuardian(input, versions) {
      await callApi(
        () =>
          declareGuardianConsent({
            guardianName: input.guardianName.trim(),
            guardianEmail: input.guardianEmail.trim(),
            relationship: input.relationship,
            docVersion: versions.guardianConsent,
          }),
        NoContent,
      );
    },
  };
}
