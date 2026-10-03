import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, renderHook, waitFor } from "@testing-library/react-native";
import type { ReactNode } from "react";
import { RepositoriesProvider, type Repositories } from "../../../providers/repositories";
import { ApiFailure } from "../../../shared/lib/http";
import { createFakeAccount } from "../../auth/data/fakes/fake-account";
import { createFakeSession } from "../../auth/data/fakes/fake-session";
import type { ClientItem, LinkId } from "../../clients/domain/client";
import type { ClientsRepository } from "../../clients/domain/ports";
import { useClients, useInviteClient, useLinkCommand } from "../../clients/hooks/use-clients";
import type { MyLink } from "../domain/invite";
import type { InviteRepository } from "../domain/ports";
import { useAcceptInvite, useEndMyLink, useInvitePreview, useMyLinks } from "./use-invite";

// Convite de ponta a ponta com um backend em memória: o profissional convida, o aluno vê a
// prévia e aceita, e as duas telas refletem o vínculo (FRONTEND-PATTERN, seção 12).

const EXPIRES = new Date("2026-10-09T12:00:00Z");

function conflict(code: string): ApiFailure {
  return new ApiFailure({ kind: "problem", status: 409, code, traceId: "t" });
}

function createCoachingBackend() {
  const links = new Map<LinkId, ClientItem>();
  let sequence = 0;

  const clients: ClientsRepository = {
    list: () => Promise.resolve({ items: [...links.values()], nextCursor: null }),
    seats: () =>
      Promise.resolve({
        active: [...links.values()].filter((link) => link.status === "active").length,
        limit: 10,
      }),
    invite: (input) => {
      sequence += 1;
      const linkId = `link-${String(sequence)}` as LinkId;
      const code = `ABCDEFG${String(sequence + 1)}`;
      links.set(linkId, {
        linkId,
        clientId: `client-${String(sequence)}`,
        name: input.name,
        status: "pending",
        startedAt: null,
        pendingInvite: { code, expiresAt: EXPIRES },
      });
      return Promise.resolve({ linkId, code, url: `https://x.test/i/${code}`, expiresAt: EXPIRES });
    },
    resendInvite: () => Promise.reject(new Error("fora deste teste")),
    cancelInvite: () => Promise.reject(new Error("fora deste teste")),
    inactivate: (linkId) => {
      const link = links.get(linkId);
      if (link?.status !== "active") {
        return Promise.reject(conflict("link-state-invalid"));
      }
      links.set(linkId, { ...link, status: "inactive" });
      return Promise.resolve();
    },
    reactivate: () => Promise.reject(new Error("fora deste teste")),
    end: () => Promise.reject(new Error("fora deste teste")),
  };

  const byCode = (code: string) =>
    [...links.values()].find((link) => link.pendingInvite?.code === code);

  const invite: InviteRepository = {
    preview: (code) =>
      byCode(code) === undefined
        ? Promise.reject(conflict("invite-expired"))
        : Promise.resolve({
            professionalName: "Ana Souza",
            organizationName: "Studio Ana",
            expiresAt: EXPIRES,
          }),
    accept: (code) => {
      const link = byCode(code);
      if (link === undefined) {
        return Promise.reject(conflict("invite-expired"));
      }
      links.set(link.linkId, {
        ...link,
        status: "active",
        startedAt: new Date(),
        pendingInvite: null,
      });
      return Promise.resolve(link.linkId);
    },
    myLinks: () =>
      Promise.resolve(
        [...links.values()]
          .filter((link) => link.status !== "pending")
          .map((link): MyLink => ({
            linkId: link.linkId,
            status: link.status,
            startedAt: link.startedAt,
            professionalName: "Ana Souza",
            organizationName: "Studio Ana",
          })),
      ),
    endMyLink: (linkId) => {
      links.delete(linkId as LinkId);
      return Promise.resolve();
    },
  };
  return { clients, invite };
}

function setup() {
  const session = createFakeSession();
  const account = createFakeAccount(session);
  const backend = createCoachingBackend();
  const repositories: Repositories = {
    session,
    me: account,
    account,
    ...backend,
    exercises: {
      search: () => Promise.reject(new Error("fora deste teste")),
      create: () => Promise.reject(new Error("fora deste teste")),
      archive: () => Promise.reject(new Error("fora deste teste")),
    },
    training: {} as Repositories["training"],
    syncApi: {} as Repositories["syncApi"],
    plannedStore: {} as Repositories["plannedStore"],
    sessionStore: {} as Repositories["sessionStore"],
    sessionSyncApi: {} as Repositories["sessionSyncApi"],
    restAlarm: { schedule: () => Promise.resolve(null), cancel: () => Promise.resolve() },
  };
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: Number.POSITIVE_INFINITY },
      mutations: { gcTime: Number.POSITIVE_INFINITY },
    },
  });
  function wrapper({ children }: { readonly children: ReactNode }) {
    return (
      <QueryClientProvider client={queryClient}>
        <RepositoriesProvider repositories={repositories}>{children}</RepositoriesProvider>
      </QueryClientProvider>
    );
  }
  return { wrapper, queryClient };
}

function useFlow() {
  return {
    clients: useClients(),
    inviteClient: useInviteClient(),
    command: useLinkCommand(),
    myLinks: useMyLinks(),
    accept: useAcceptInvite(),
    endMyLink: useEndMyLink(),
  };
}

describe("convite de ponta a ponta", () => {
  it("profissional convida, aluno aceita, lista e casa do aluno refletem o vínculo", async () => {
    const { wrapper, queryClient } = setup();
    const { result } = await renderHook(() => useFlow(), { wrapper });
    const idle = await renderHook(() => useInvitePreview(null), { wrapper });
    expect(idle.result.current.fetchStatus).toBe("idle"); // sem código, não busca

    await waitFor(() => {
      expect(result.current.clients.data?.pages[0]?.items).toEqual([]);
    });

    const invitation = await act(() =>
      result.current.inviteClient.mutateAsync({
        name: "Bia Lima",
        email: null,
        phone: null,
        goal: null,
      }),
    );
    await waitFor(() => {
      expect(result.current.clients.data?.pages[0]?.items[0]?.status).toBe("pending");
    });

    const preview = await renderHook(() => useInvitePreview(invitation.code), { wrapper });
    await waitFor(() => {
      expect(preview.result.current.data?.professionalName).toBe("Ana Souza");
    });

    await act(() => result.current.accept.mutateAsync(invitation.code));
    await waitFor(() => {
      expect(result.current.myLinks.data?.[0]?.status).toBe("active");
    });

    // a lista do profissional muda em outro aparelho: aqui, depois de invalidar
    await act(() => queryClient.invalidateQueries({ queryKey: ["clients"] }));
    await waitFor(() => {
      expect(result.current.clients.data?.pages[0]?.items[0]?.status).toBe("active");
    });

    await act(() =>
      result.current.command.mutateAsync({ command: "inactivate", linkId: invitation.linkId }),
    );
    await waitFor(() => {
      expect(result.current.clients.data?.pages[0]?.items[0]?.status).toBe("inactive");
    });

    await act(() => result.current.endMyLink.mutateAsync(invitation.linkId));
    await waitFor(() => {
      expect(result.current.myLinks.data).toEqual([]);
    });
  });

  it("convite usado não aceita de novo", async () => {
    const { wrapper } = setup();
    const { result } = await renderHook(() => useFlow(), { wrapper });

    await expect(act(() => result.current.accept.mutateAsync("ZZZZZZZZ"))).rejects.toMatchObject({
      error: { code: "invite-expired" },
    });
  });
});
