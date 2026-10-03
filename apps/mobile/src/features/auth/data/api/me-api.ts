import { getMe, schemas } from "@moveup/api-client";
import type { z } from "zod";
import { callApi } from "../../../../shared/lib/http";
import type { Me, UserId } from "../../domain/me";
import type { MeRepository } from "../../domain/ports";

type MeDto = z.infer<typeof schemas.GetMe200Response>;

/** DTO da API → domínio. O DTO não sai da camada data. */
export function toMe(dto: MeDto): Me {
  return {
    // Zod já validou o formato uuid; o brand só marca a origem.
    id: dto.id as UserId,
    name: dto.name,
    email: dto.email,
    locale: dto.locale,
    timezone: dto.timezone,
    units: { weight: dto.weightUnit, length: dto.lengthUnit },
    role: dto.role ?? null,
    isMinor: dto.minor,
    onboarding: {
      missingConsents: dto.missingConsents,
      guardianConsentRequired: dto.guardianConsentRequired,
      guardianRequest:
        dto.guardianRequest == null
          ? null
          : {
              status: dto.guardianRequest.status,
              guardianName: dto.guardianRequest.guardianName,
              relationship: dto.guardianRequest.relationship,
              requestedAt: new Date(dto.guardianRequest.requestedAt),
              linkExpiresAt:
                dto.guardianRequest.linkExpiresAt == null
                  ? null
                  : new Date(dto.guardianRequest.linkExpiresAt),
            },
    },
  };
}

export function createMeApiRepository(): MeRepository {
  return {
    async getMe() {
      return toMe(await callApi(() => getMe(), schemas.GetMe200Response));
    },
  };
}
