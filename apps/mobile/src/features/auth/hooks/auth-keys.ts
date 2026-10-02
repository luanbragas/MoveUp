// Fábrica de query keys da feature (FRONTEND-PATTERN, seção 8): nunca array à mão no componente.
export const authKeys = {
  all: ["auth"] as const,
  /** Por usuário do provedor: trocar de conta nunca reaproveita o cache de outra. */
  me: (uid: string) => [...authKeys.all, "me", uid] as const,
  legalVersions: () => [...authKeys.all, "legal-versions"] as const,
};
