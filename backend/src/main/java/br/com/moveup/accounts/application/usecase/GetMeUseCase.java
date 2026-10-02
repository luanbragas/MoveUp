package br.com.moveup.accounts.application.usecase;

import br.com.moveup.accounts.application.port.in.GetMe;
import br.com.moveup.accounts.application.port.in.MeView;
import br.com.moveup.accounts.application.port.out.AccountReader;
import br.com.moveup.accounts.application.port.out.ConsentRepository;
import br.com.moveup.accounts.application.port.out.GuardianConsentRepository;
import br.com.moveup.accounts.application.port.out.LegalDocuments;
import br.com.moveup.accounts.domain.exception.AccountNotRegistered;
import br.com.moveup.accounts.domain.model.ConsentKind;
import java.time.Clock;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class GetMeUseCase implements GetMe {

  private final AccountReader accounts;
  private final ConsentRepository consents;
  private final GuardianConsentRepository guardianConsents;
  private final LegalDocuments legalDocuments;
  private final Clock clock;

  public GetMeUseCase(
      AccountReader accounts,
      ConsentRepository consents,
      GuardianConsentRepository guardianConsents,
      LegalDocuments legalDocuments,
      Clock clock) {
    this.accounts = accounts;
    this.consents = consents;
    this.guardianConsents = guardianConsents;
    this.legalDocuments = legalDocuments;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public MeView handle(UUID userId) {
    var summary = accounts.find(userId).orElseThrow(AccountNotRegistered::new);
    var onboarding =
        Profiles.of(summary)
            .onboardingOn(
                Profiles.today(clock),
                consents.acceptedVersions(userId),
                guardianConsents.hasActive(userId),
                legalDocuments.current());
    return new MeView(
        summary.id(),
        summary.name(),
        summary.email(),
        summary.locale(),
        summary.timezone(),
        summary.weightUnit(),
        summary.lengthUnit(),
        summary.role(),
        onboarding.minor(),
        onboarding.missingConsents().stream().map(ConsentKind::code).sorted().toList(),
        onboarding.guardianConsentRequired());
  }
}
