import type { AppError } from "../lib/http";

// Mensagens ao usuário por `code` do backend (FRONTEND-PATTERN, seção 9).
// Todo `code` novo do backend ganha uma linha aqui.
const BY_CODE: Readonly<Record<string, string>> = {
  unauthenticated: "Sua sessão expirou. Entre de novo.",
  forbidden: "Você não tem acesso a isso.",
  "resource-not-found": "Não encontramos o que você procurava.",
  "account-not-registered": "Ainda não encontramos seu cadastro. Vamos criar sua conta?",
  "validation-failed": "Confira os campos destacados.",
  "malformed-request": "Não foi possível enviar os dados. Tente de novo.",
  "invite-expired": "Este convite não é mais válido. Peça um novo ao seu personal.",
  "plan-limit-reached": "O plano do seu personal atingiu o limite de alunos.",
  "client-already-linked": "Você já tem um personal ativo no MoveUp.",
  "version-mismatch": "Este conteúdo foi alterado em outro aparelho. Atualize e tente de novo.",
  "state-conflict": "Essa ação não é possível agora.",
  "rate-limited": "Muitas tentativas. Aguarde um pouco.",
};

const NETWORK = "Sem conexão com a internet.";
const FALLBACK = "Algo deu errado. Tente de novo.";

export function errorMessage(error: AppError): string {
  switch (error.kind) {
    case "problem":
      return BY_CODE[error.code] ?? FALLBACK;
    case "network":
      return NETWORK;
    case "unexpected":
      return FALLBACK;
  }
}
