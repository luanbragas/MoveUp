import {
  cancelInvite,
  endLink,
  getClientSeats,
  inactivateLink,
  inviteClient,
  listClients,
  reactivateLink,
  resendInvite,
  schemas,
  type NewClient,
} from "@moveup/api-client";
import { z } from "zod";
import { callApi } from "../../../../shared/lib/http";
import type { ClientItem, Invitation, LinkId } from "../../domain/client";
import type { ClientsRepository } from "../../domain/ports";

const NoContent = z.undefined();
const PAGE_SIZE = 50;

type InvitationDto = z.infer<typeof schemas.InviteClient201Response>;
type ClientDto = z.infer<typeof schemas.ListClients200Response>["items"][number];

function toInvitation(dto: InvitationDto): Invitation {
  return {
    linkId: dto.linkId as LinkId, // Zod já validou o uuid
    code: dto.code,
    url: dto.url,
    expiresAt: new Date(dto.expiresAt),
  };
}

function toClient(dto: ClientDto): ClientItem {
  return {
    linkId: dto.linkId as LinkId,
    clientId: dto.clientId,
    name: dto.name,
    status: dto.status,
    startedAt: dto.startedAt == null ? null : new Date(dto.startedAt),
    pendingInvite:
      dto.pendingInvite == null
        ? null
        : { code: dto.pendingInvite.code, expiresAt: new Date(dto.pendingInvite.expiresAt) },
  };
}

function optional(value: string | null): string | undefined {
  return value === null || value.trim() === "" ? undefined : value.trim();
}

export function createClientsApiRepository(): ClientsRepository {
  return {
    async list(cursor) {
      const page = await callApi(
        () => listClients(cursor === null ? { limit: PAGE_SIZE } : { cursor, limit: PAGE_SIZE }),
        schemas.ListClients200Response,
      );
      return { items: page.items.map(toClient), nextCursor: page.nextCursor ?? null };
    },
    async seats() {
      const dto = await callApi(() => getClientSeats(), schemas.GetClientSeats200Response);
      return { active: dto.active, limit: dto.limit ?? null };
    },

    async invite(input) {
      const body: NewClient = { name: input.name.trim() };
      const email = optional(input.email);
      const phone = optional(input.phone);
      const goal = optional(input.goal);
      if (email !== undefined) {
        body.email = email;
      }
      if (phone !== undefined) {
        body.phone = phone;
      }
      if (goal !== undefined) {
        body.goal = goal;
      }
      return toInvitation(await callApi(() => inviteClient(body), schemas.InviteClient201Response));
    },
    async resendInvite(linkId) {
      return toInvitation(
        await callApi(() => resendInvite(linkId), schemas.ResendInvite200Response),
      );
    },
    async cancelInvite(linkId) {
      await callApi(() => cancelInvite(linkId), NoContent);
    },
    async inactivate(linkId) {
      await callApi(() => inactivateLink(linkId), NoContent);
    },
    async reactivate(linkId) {
      await callApi(() => reactivateLink(linkId), NoContent);
    },
    async end(linkId) {
      await callApi(() => endLink(linkId), NoContent);
    },
  };
}
