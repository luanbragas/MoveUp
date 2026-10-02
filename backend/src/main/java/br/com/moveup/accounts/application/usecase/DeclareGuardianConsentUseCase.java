package br.com.moveup.accounts.application.usecase;

import br.com.moveup.accounts.application.port.in.DeclareGuardianConsent;
import br.com.moveup.accounts.application.port.out.AccountReader;
import br.com.moveup.accounts.application.port.out.GuardianConsentRepository;
import br.com.moveup.accounts.application.port.out.LegalDocuments;
import br.com.moveup.accounts.domain.exception.AccountNotRegistered;
import br.com.moveup.accounts.domain.exception.GuardianConsentNotAllowed;
import br.com.moveup.accounts.domain.exception.InvalidAccountData;
import br.com.moveup.accounts.domain.model.Email;
import br.com.moveup.accounts.domain.model.GuardianConsent;
import br.com.moveup.accounts.domain.model.GuardianRelationship;
import br.com.moveup.accounts.domain.model.PersonName;
import br.com.moveup.shared.domain.IdGenerator;
import java.time.Clock;
import org.springframework.transaction.annotation.Transactional;

public class DeclareGuardianConsentUseCase implements DeclareGuardianConsent {

  private final AccountReader accounts;
  private final GuardianConsentRepository guardianConsents;
  private final LegalDocuments legalDocuments;
  private final IdGenerator ids;
  private final Clock clock;

  public DeclareGuardianConsentUseCase(
      AccountReader accounts,
      GuardianConsentRepository guardianConsents,
      LegalDocuments legalDocuments,
      IdGenerator ids,
      Clock clock) {
    this.accounts = accounts;
    this.guardianConsents = guardianConsents;
    this.legalDocuments = legalDocuments;
    this.ids = ids;
    this.clock = clock;
  }

  @Override
  @Transactional
  public void handle(Command command) {
    var minor = Profiles.of(accounts.find(command.userId()).orElseThrow(AccountNotRegistered::new));
    if (guardianConsents.hasActive(command.userId())) {
      throw GuardianConsentNotAllowed.alreadyActive();
    }
    var consent =
        GuardianConsent.declare(
            ids.newId(),
            minor,
            PersonName.of(command.guardianName()),
            Email.of(command.guardianEmail()),
            parseRelationship(command.relationship()),
            command.docVersion(),
            legalDocuments.current(),
            clock.instant(),
            Profiles.today(clock));
    guardianConsents.save(consent, command.origin());
  }

  private static GuardianRelationship parseRelationship(String relationship) {
    try {
      return GuardianRelationship.fromCode(relationship);
    } catch (IllegalArgumentException e) {
      throw new InvalidAccountData("relationship-invalid", "Parentesco inválido.");
    }
  }
}
