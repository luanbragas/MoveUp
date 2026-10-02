package br.com.moveup.accounts.domain.exception;

import br.com.moveup.shared.domain.ConflictException;

/**
 * Consentimento do responsável fora de lugar: a conta não é de menor ({@code
 * guardian-consent-not-required}) ou já tem um vigente ({@code guardian-consent-already-active}).
 */
public final class GuardianConsentNotAllowed extends ConflictException {

  public static final String NOT_REQUIRED = "guardian-consent-not-required";
  public static final String ALREADY_ACTIVE = "guardian-consent-already-active";

  private GuardianConsentNotAllowed(String code, String safeMessage) {
    super(code, safeMessage);
  }

  public static GuardianConsentNotAllowed notRequired() {
    return new GuardianConsentNotAllowed(
        NOT_REQUIRED, "Consentimento do responsável só é necessário para menores de 18 anos.");
  }

  public static GuardianConsentNotAllowed alreadyActive() {
    return new GuardianConsentNotAllowed(
        ALREADY_ACTIVE, "Já existe um consentimento do responsável vigente.");
  }
}
