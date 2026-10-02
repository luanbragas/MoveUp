// Fábrica de query keys da feature (FRONTEND-PATTERN, seção 8): nunca array à mão no componente.
export const authKeys = {
  all: ["auth"] as const,
  me: () => [...authKeys.all, "me"] as const,
};
