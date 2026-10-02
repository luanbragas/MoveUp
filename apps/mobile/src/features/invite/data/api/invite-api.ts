import {
  acceptInvite,
  endMyLink,
  getMyCoachingLinks,
  previewInvite,
  schemas,
} from "@moveup/api-client";
import { z } from "zod";
import { callApi } from "../../../../shared/lib/http";
import type { InviteRepository } from "../../domain/ports";

const NoContent = z.undefined();

export function createInviteApiRepository(): InviteRepository {
  return {
    async preview(code) {
      const dto = await callApi(() => previewInvite(code), schemas.PreviewInvite200Response);
      return {
        professionalName: dto.professionalName,
        organizationName: dto.organizationName,
        expiresAt: new Date(dto.expiresAt),
      };
    },
    async accept(code) {
      const dto = await callApi(() => acceptInvite(code), schemas.AcceptInvite200Response);
      return dto.linkId;
    },
    async myLinks() {
      const items = await callApi(
        () => getMyCoachingLinks(),
        schemas.GetMyCoachingLinks200Response,
      );
      return items.map((dto) => ({
        linkId: dto.linkId,
        status: dto.status,
        startedAt: dto.startedAt == null ? null : new Date(dto.startedAt),
        professionalName: dto.professionalName,
        organizationName: dto.organizationName,
      }));
    },
    async endMyLink(linkId) {
      await callApi(() => endMyLink(linkId), NoContent);
    },
  };
}
