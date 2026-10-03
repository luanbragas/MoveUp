package br.com.moveup.accounts.application.usecase;

import br.com.moveup.accounts.application.port.in.GuardianAuthorization;
import br.com.moveup.accounts.application.port.out.GuardianConsentRepository;
import br.com.moveup.accounts.application.port.out.GuardianConsentRepository.LinkedRequest;
import br.com.moveup.accounts.application.port.out.LegalDocuments;
import br.com.moveup.accounts.domain.exception.GuardianAuthorizationNotFound;
import br.com.moveup.accounts.domain.model.GuardianLinkToken;
import java.time.Clock;
import org.springframework.transaction.annotation.Transactional;

public class GuardianAuthorizationUseCase implements GuardianAuthorization {

  private final GuardianConsentRepository guardianConsents;
  private final LegalDocuments legalDocuments;
  private final Clock clock;

  public GuardianAuthorizationUseCase(
      GuardianConsentRepository guardianConsents, LegalDocuments legalDocuments, Clock clock) {
    this.guardianConsents = guardianConsents;
    this.legalDocuments = legalDocuments;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public Preview preview(String token) {
    var linked = find(token);
    var consent = linked.request().consent();
    return new Preview(
        linked.request().minorFirstName(),
        consent.guardianName().value(),
        consent.relationship().code(),
        legalDocuments.current().guardianConsent(),
        consent.linkExpiresAt());
  }

  @Override
  @Transactional
  public void decide(Decision decision) {
    var linked = find(decision.token());
    var consent = linked.request().consent();
    var now = clock.instant();
    var decided =
        decision.approve()
            ? consent.approve(decision.docVersion(), legalDocuments.current(), now)
            : consent.decline(now);
    if (!guardianConsents.recordDecision(decided, linked.token(), decision.origin())) {
      throw new GuardianAuthorizationNotFound(); // decidido em paralelo (ex.: duplo toque)
    }
  }

  private Found find(String rawToken) {
    var token = GuardianLinkToken.parse(rawToken).orElseThrow(GuardianAuthorizationNotFound::new);
    var request =
        guardianConsents
            .findByLink(token)
            .filter(r -> r.consent().linkOpenAt(clock.instant()))
            .orElseThrow(GuardianAuthorizationNotFound::new);
    return new Found(token, request);
  }

  private record Found(GuardianLinkToken token, LinkedRequest request) {}
}
