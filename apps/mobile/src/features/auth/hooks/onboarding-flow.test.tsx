import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, renderHook, waitFor } from "@testing-library/react-native";
import type { ReactNode } from "react";
import { RepositoriesProvider, type Repositories } from "../../../providers/repositories";
import { FAKE_VERSIONS, createFakeAccount } from "../data/fakes/fake-account";
import { createFakeSession } from "../data/fakes/fake-session";
import { useSignOut, useSignUp } from "./use-auth-actions";
import { useEntry } from "./use-entry";
import {
  useCancelGuardianRequest,
  useGrantConsents,
  useRegisterAccount,
  useRequestGuardian,
  useResendGuardianLink,
} from "./use-onboarding";

// Fluxo de entrada no app com fakes (FRONTEND-PATTERN, seção 12: hooks com repositórios fake).

function setup() {
  const session = createFakeSession();
  const fake = createFakeAccount(session);
  const unused = () => Promise.reject(new Error("fora deste teste"));
  const repositories: Repositories = {
    session,
    me: fake,
    account: fake,
    clients: {
      list: unused,
      invite: unused,
      resendInvite: unused,
      cancelInvite: unused,
      inactivate: unused,
      reactivate: unused,
      end: unused,
    },
    invite: { preview: unused, accept: unused, myLinks: unused, endMyLink: unused },
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
  return { session, fake, wrapper };
}

function useFlow(uid: string) {
  return {
    entry: useEntry(),
    signUp: useSignUp(),
    signOut: useSignOut(),
    register: useRegisterAccount(uid),
    grant: useGrantConsents(uid),
    guardian: useRequestGuardian(uid),
    resend: useResendGuardianLink(uid),
    cancel: useCancelGuardianRequest(uid),
  };
}

const EMAIL = "ana@example.test";
const UID = `uid-${EMAIL}`;

describe("entrada no app", () => {
  it("aluno menor: login, cadastro, aceites, pedido ao responsável e casa do aluno", async () => {
    const { fake, wrapper } = setup();
    const { result } = await renderHook(() => useFlow(UID), { wrapper });

    await waitFor(() => {
      expect(result.current.entry.destination).toBe("welcome");
    });

    await act(() => result.current.signUp.mutateAsync({ email: EMAIL, password: "senha-forte-1" }));
    await waitFor(() => {
      expect(result.current.entry.destination).toBe("register");
    });

    await act(() =>
      result.current.register.mutateAsync({
        role: "client",
        name: "Ana",
        birthDate: `${String(new Date().getUTCFullYear() - 15)}-01-01`,
        businessName: null,
        registryNumber: null,
      }),
    );
    await waitFor(() => {
      expect(result.current.entry.destination).toBe("consents");
    });

    await act(() =>
      result.current.grant.mutateAsync({
        kinds: ["health_data", "privacy", "terms"],
        versions: FAKE_VERSIONS,
      }),
    );
    await waitFor(() => {
      expect(result.current.entry.destination).toBe("guardian");
    });

    const link = await act(() =>
      result.current.guardian.mutateAsync({
        input: { guardianName: "Maria", relationship: "mother" },
        versions: FAKE_VERSIONS,
      }),
    );
    expect(link.url).toContain("#");
    // pedido feito, mas só libera quando o responsável autorizar pelo link
    await waitFor(() => {
      expect(result.current.entry.me?.onboarding.guardianRequest?.status).toBe("pending");
    });
    expect(result.current.entry.destination).toBe("guardian");

    fake.guardianDecides(UID, true);
    await act(() => {
      result.current.entry.retry();
    });
    await waitFor(() => {
      expect(result.current.entry.destination).toBe("client-home");
    });
  });

  it("aluno menor: reenviar troca o link, cancelar permite indicar outra pessoa e a recusa aparece", async () => {
    const { fake, wrapper } = setup();
    const { result } = await renderHook(() => useFlow(UID), { wrapper });
    await act(() => result.current.signUp.mutateAsync({ email: EMAIL, password: "senha-forte-1" }));
    await act(() =>
      result.current.register.mutateAsync({
        role: "client",
        name: "Ana",
        birthDate: `${String(new Date().getUTCFullYear() - 15)}-01-01`,
        businessName: null,
        registryNumber: null,
      }),
    );
    await act(() =>
      result.current.grant.mutateAsync({
        kinds: ["health_data", "privacy", "terms"],
        versions: FAKE_VERSIONS,
      }),
    );
    const input = {
      input: { guardianName: "Maria", relationship: "mother" },
      versions: FAKE_VERSIONS,
    } as const;

    const first = await act(() => result.current.guardian.mutateAsync(input));
    const second = await act(() => result.current.resend.mutateAsync());
    expect(second.url).not.toBe(first.url);

    await act(() => result.current.cancel.mutateAsync());
    await waitFor(() => {
      expect(result.current.entry.me?.onboarding.guardianRequest).toBeNull();
    });

    await act(() => result.current.guardian.mutateAsync(input));
    fake.guardianDecides(UID, false);
    await act(() => {
      result.current.entry.retry();
    });
    await waitFor(() => {
      expect(result.current.entry.me?.onboarding.guardianRequest?.status).toBe("declined");
    });
    expect(result.current.entry.destination).toBe("guardian");
  });

  it("personal adulto vai direto para a casa do personal depois dos aceites; sair volta ao início", async () => {
    const { wrapper } = setup();
    const { result } = await renderHook(() => useFlow(UID), { wrapper });

    await act(() => result.current.signUp.mutateAsync({ email: EMAIL, password: "senha-forte-1" }));
    await act(() =>
      result.current.register.mutateAsync({
        role: "professional",
        name: "Carlos",
        birthDate: null,
        businessName: "Studio",
        registryNumber: null,
      }),
    );
    await act(() =>
      result.current.grant.mutateAsync({ kinds: ["privacy", "terms"], versions: FAKE_VERSIONS }),
    );
    await waitFor(() => {
      expect(result.current.entry.destination).toBe("professional-home");
    });

    await act(() => result.current.signOut.mutateAsync());
    await waitFor(() => {
      expect(result.current.entry.destination).toBe("welcome");
    });
  });
});
