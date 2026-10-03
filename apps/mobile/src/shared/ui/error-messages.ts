import type { AppError } from "../lib/http";

// Mensagens ao usuário por `code` do backend (FRONTEND-PATTERN, seção 9).
// Todo `code` novo do backend ganha uma linha aqui.
const BY_CODE: Readonly<Record<string, string>> = {
  unauthenticated: "Sua sessão expirou. Entre de novo.",
  forbidden: "Você não tem acesso a isso.",
  "resource-not-found": "Não encontramos o que você procurava.",
  "account-not-registered": "Ainda não encontramos seu cadastro. Vamos criar sua conta?",
  "account-already-registered": "Você já tem uma conta. Entre com ela.",
  "email-required": "Entre com uma conta que tenha e-mail (Google, Apple ou e-mail).",
  "email-invalid": "Confira o e-mail.",
  "name-invalid": "Informe um nome entre 2 e 200 caracteres.",
  "role-invalid": "Escolha se você é personal ou aluno.",
  "birth-date-required": "Informe sua data de nascimento.",
  "birth-date-invalid": "Confira a data de nascimento.",
  "professional-must-be-adult": "O perfil de personal exige maioridade.",
  "business-name-invalid": "Confira o nome do seu negócio.",
  "registry-number-invalid": "Confira o número do CREF.",
  "consent-version-outdated": "O texto foi atualizado. Leia a versão nova para continuar.",
  "guardian-consent-not-required": "O consentimento do responsável é só para menores de 18 anos.",
  "guardian-consent-already-active": "Você já tem um pedido aberto para o seu responsável.",
  "guardian-request-not-pending": "Esse pedido não está mais aguardando. Atualize a tela.",
  "relationship-invalid": "Escolha o parentesco do responsável.",
  "link-state-invalid": "Esse aluno não está numa situação que permita isso agora.",
  "onboarding-incomplete":
    "Termine seu cadastro (termos e autorizações) antes de aceitar o convite.",
  "invite-for-clients-only": "Convites são para contas de aluno.",
  "phone-invalid": "Confira o WhatsApp, com DDD.",
  "goal-invalid": "O objetivo pode ter até 500 caracteres.",
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
