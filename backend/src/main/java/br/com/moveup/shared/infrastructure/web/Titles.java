package br.com.moveup.shared.infrastructure.web;

import java.util.Map;

/**
 * Catálogo de títulos legíveis por {@code code} de erro (pt-BR, pronto para i18n). O app decide a
 * mensagem pelo {@code code}; o título serve para log, suporte e clientes genéricos.
 */
public final class Titles {

  static final String FALLBACK = "Não foi possível concluir a operação";

  private static final Map<String, String> BY_CODE =
      Map.ofEntries(
          Map.entry("validation-failed", "Dados inválidos"),
          Map.entry("malformed-request", "Requisição malformada"),
          Map.entry("resource-not-found", "Recurso não encontrado"),
          Map.entry("account-not-registered", "Conta não cadastrada"),
          Map.entry("account-already-registered", "Conta já cadastrada"),
          Map.entry("email-required", "E-mail obrigatório"),
          Map.entry("email-invalid", "E-mail inválido"),
          Map.entry("name-invalid", "Nome inválido"),
          Map.entry("role-invalid", "Perfil inválido"),
          Map.entry("birth-date-required", "Data de nascimento obrigatória"),
          Map.entry("birth-date-invalid", "Data de nascimento inválida"),
          Map.entry("professional-must-be-adult", "Perfil de profissional exige maioridade"),
          Map.entry("business-name-invalid", "Nome do negócio inválido"),
          Map.entry("registry-number-invalid", "CREF inválido"),
          Map.entry("consent-version-outdated", "Texto legal atualizado"),
          Map.entry("consent-kind-invalid", "Tipo de consentimento inválido"),
          Map.entry("consents-empty", "Nenhum consentimento informado"),
          Map.entry("guardian-consent-not-required", "Consentimento do responsável não se aplica"),
          Map.entry("guardian-consent-already-active", "Pedido ao responsável já registrado"),
          Map.entry("guardian-request-not-pending", "Nenhum pedido aguardando o responsável"),
          Map.entry("guardian-authorization-not-found", "Link de autorização inválido ou vencido"),
          Map.entry("relationship-invalid", "Parentesco inválido"),
          Map.entry("link-state-invalid", "O vínculo não permite essa ação agora"),
          Map.entry("onboarding-incomplete", "Cadastro incompleto"),
          Map.entry("invite-for-clients-only", "Convite só para contas de aluno"),
          Map.entry("phone-invalid", "Telefone inválido"),
          Map.entry("goal-invalid", "Objetivo inválido"),
          Map.entry("method-not-allowed", "Método não permitido"),
          Map.entry("unsupported-media-type", "Formato de conteúdo não suportado"),
          Map.entry("not-acceptable", "Formato de resposta não suportado"),
          Map.entry("payload-too-large", "Conteúdo grande demais"),
          Map.entry("unauthenticated", "Autenticação necessária"),
          Map.entry("forbidden", "Acesso negado"),
          Map.entry("version-mismatch", "O recurso foi alterado por outra pessoa"),
          Map.entry("invite-expired", "Convite inválido ou expirado"),
          Map.entry("plan-limit-reached", "Limite de alunos do plano atingido"),
          Map.entry("client-already-linked", "Aluno já tem vínculo ativo com outro profissional"),
          Map.entry("state-conflict", "A operação não é permitida no estado atual"),
          Map.entry("already-exists", "Registro já existe"),
          Map.entry("invalid-reference", "Referência inválida"),
          Map.entry("rate-limited", "Muitas requisições"),
          Map.entry("exercise-name-invalid", "Nome do exercício inválido"),
          Map.entry("exercise-name-taken", "Exercício com esse nome já existe"),
          Map.entry("base-exercise-read-only", "Exercício da biblioteca base"),
          Map.entry("modality-invalid", "Modalidade inválida"),
          Map.entry("tracking-type-invalid", "Tipo de registro inválido"),
          Map.entry("muscle-invalid", "Músculo inválido"),
          Map.entry("instructions-invalid", "Instruções inválidas"),
          Map.entry("media-url-invalid", "Link do vídeo inválido"),
          Map.entry("internal-error", "Erro inesperado"));

  private Titles() {}

  public static String of(String code) {
    return BY_CODE.getOrDefault(code, FALLBACK);
  }
}
