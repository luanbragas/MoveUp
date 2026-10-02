package br.com.moveup.accounts.domain.model;

import br.com.moveup.accounts.domain.exception.ConsentVersionOutdated;
import br.com.moveup.accounts.domain.exception.GuardianConsentNotAllowed;
import br.com.moveup.accounts.domain.exception.InvalidAccountData;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Declaração de consentimento de um dos pais ou do responsável legal pelo aluno menor (LGPD, art.
 * 14). Só existe para conta de menor, com o texto vigente, e com e-mail diferente do próprio menor.
 */
public record GuardianConsent(
    UUID id,
    UUID userId,
    PersonName guardianName,
    Email guardianEmail,
    GuardianRelationship relationship,
    String docVersion,
    Instant grantedAt) {

  public static GuardianConsent declare(
      UUID id,
      AccountProfile minor,
      PersonName guardianName,
      Email guardianEmail,
      GuardianRelationship relationship,
      String docVersion,
      LegalVersions current,
      Instant now,
      LocalDate today) {
    if (!minor.isMinorOn(today)) {
      throw GuardianConsentNotAllowed.notRequired();
    }
    if (!current.guardianConsent().equals(docVersion)) {
      throw new ConsentVersionOutdated();
    }
    if (guardianEmail.equals(minor.email())) {
      throw new InvalidAccountData(
          "guardian-email-invalid", "Informe o e-mail do responsável, não o seu.");
    }
    return new GuardianConsent(
        id, minor.userId(), guardianName, guardianEmail, relationship, docVersion, now);
  }
}
