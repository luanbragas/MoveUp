package br.com.moveup.accounts.application.usecase;

import br.com.moveup.accounts.api.AccountDirectory;
import br.com.moveup.accounts.application.port.out.AccountReader;
import br.com.moveup.accounts.application.port.out.ConsentRepository;
import br.com.moveup.accounts.application.port.out.GuardianConsentRepository;
import br.com.moveup.accounts.application.port.out.LegalDocuments;
import br.com.moveup.accounts.application.port.out.OrganizationReader;
import br.com.moveup.accounts.domain.model.AccountRole;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/** Implementa a porta pública do módulo; roda na transação de quem chama. */
public class AccountDirectoryService implements AccountDirectory {

  private final AccountReader accounts;
  private final OrganizationReader organizations;
  private final ConsentRepository consents;
  private final GuardianConsentRepository guardianConsents;
  private final LegalDocuments legalDocuments;
  private final Clock clock;

  public AccountDirectoryService(
      AccountReader accounts,
      OrganizationReader organizations,
      ConsentRepository consents,
      GuardianConsentRepository guardianConsents,
      LegalDocuments legalDocuments,
      Clock clock) {
    this.accounts = accounts;
    this.organizations = organizations;
    this.consents = consents;
    this.guardianConsents = guardianConsents;
    this.legalDocuments = legalDocuments;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<UUID> organizationOf(UUID professionalUserId) {
    return organizations.ownedBy(professionalUserId);
  }

  @Override
  @Transactional(readOnly = true)
  public LinkReadiness linkReadiness(UUID userId) {
    var summary = accounts.find(userId);
    if (summary.isEmpty()) {
      return LinkReadiness.NOT_A_CLIENT;
    }
    var profile = Profiles.of(summary.get());
    if (profile.role().filter(AccountRole.CLIENT::equals).isEmpty()) {
      return LinkReadiness.NOT_A_CLIENT;
    }
    var onboarding =
        profile.onboardingOn(
            Profiles.today(clock),
            consents.acceptedVersions(userId),
            guardianConsents.hasActive(userId),
            legalDocuments.current());
    return onboarding.complete() ? LinkReadiness.READY : LinkReadiness.ONBOARDING_INCOMPLETE;
  }
}
