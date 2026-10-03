package br.com.moveup.accounts.domain.exception;

import br.com.moveup.shared.domain.ConflictException;

/**
 * Consentimento do responsável fora de lugar: a conta não é de menor ({@code
 * guardian-consent-not-required}), já tem um pedido ou consentimento vigente ({@code
 * guardian-consent-already-active}) ou não tem pedido aguardando ({@code
 * guardian-request-not-pending}).
 */
public final class GuardianConsentNotAllowed extends ConflictException {

  public static final String NOT_REQUIRED = "guardian-consent-not-required";
  public static final String ALREADY_ACTIVE = "guardian-consent-already-active";
  public static final String NOT_PENDING = "guardian-request-not-pending";

  private GuardianConsentNotAllowed(String code, String safeMessage) {
    super(code, safeMessage);
  }

  public static GuardianConsentNotAllowed notRequired() {
    return new GuardianConsentNotAllowed(
        NOT_REQUIRED, "Consentimento do responsável só é necessário para menores de 18 anos.");
  }

  public static GuardianConsentNotAllowed alreadyActive() {
    return new GuardianConsentNotAllowed(
        ALREADY_ACTIVE, "Já existe um pedido ou consentimento do responsável vigente.");
  }

  public static GuardianConsentNotAllowed notPending() {
    return new GuardianConsentNotAllowed(NOT_PENDING, "Não há pedido aguardando o responsável.");
  }
}
