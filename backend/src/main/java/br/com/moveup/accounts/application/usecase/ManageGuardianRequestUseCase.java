package br.com.moveup.accounts.application.usecase;

import br.com.moveup.accounts.application.port.in.ManageGuardianRequest;
import br.com.moveup.accounts.application.port.out.AccountReader;
import br.com.moveup.accounts.application.port.out.GuardianConsentRepository;
import br.com.moveup.accounts.application.port.out.LegalDocuments;
import br.com.moveup.accounts.domain.exception.AccountNotRegistered;
import br.com.moveup.accounts.domain.exception.GuardianConsentNotAllowed;
import br.com.moveup.accounts.domain.exception.InvalidAccountData;
import br.com.moveup.accounts.domain.model.GuardianConsent;
import br.com.moveup.accounts.domain.model.GuardianLinkToken;
import br.com.moveup.accounts.domain.model.GuardianRelationship;
import br.com.moveup.accounts.domain.model.PersonName;
import br.com.moveup.shared.domain.IdGenerator;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Consumer;
import org.springframework.transaction.annotation.Transactional;

public class ManageGuardianRequestUseCase implements ManageGuardianRequest {

  private final AccountReader accounts;
  private final GuardianConsentRepository guardianConsents;
  private final LegalDocuments legalDocuments;
  private final IdGenerator ids;
  private final Consumer<byte[]> randomBytes;
  private final Clock clock;
  private final Duration linkTtl;
  private final String linkBaseUrl;

  /**
   * @param randomBytes fonte do segredo do link (na infraestrutura, {@code SecureRandom})
   * @param linkBaseUrl página do responsável, ex.: {@code
   *     https://moveup-site.pages.dev/autorizar/}; o segredo vai depois de {@code #}, que o
   *     navegador não manda ao servidor do site
   */
  public ManageGuardianRequestUseCase(
      AccountReader accounts,
      GuardianConsentRepository guardianConsents,
      LegalDocuments legalDocuments,
      IdGenerator ids,
      Consumer<byte[]> randomBytes,
      Clock clock,
      Duration linkTtl,
      String linkBaseUrl) {
    if (linkTtl.isNegative() || linkTtl.isZero()) {
      throw new IllegalArgumentException("validade do link precisa ser positiva");
    }
    this.accounts = accounts;
    this.guardianConsents = guardianConsents;
    this.legalDocuments = legalDocuments;
    this.ids = ids;
    this.randomBytes = randomBytes;
    this.clock = clock;
    this.linkTtl = linkTtl;
    this.linkBaseUrl = linkBaseUrl;
  }

  @Override
  @Transactional
  public GuardianLink request(RequestCommand command) {
    var minor = Profiles.of(accounts.find(command.userId()).orElseThrow(AccountNotRegistered::new));
    if (guardianConsents.latest(command.userId()).filter(GuardianConsent::isOpen).isPresent()) {
      throw GuardianConsentNotAllowed.alreadyActive();
    }
    var requested =
        GuardianConsent.request(
            ids.newId(),
            minor,
            PersonName.of(command.guardianName()),
            parseRelationship(command.relationship()),
            command.docVersion(),
            legalDocuments.current(),
            clock.instant(),
            Profiles.today(clock));
    guardianConsents.save(requested, command.origin());
    return issueLink(requested);
  }

  @Override
  @Transactional
  public GuardianLink resendLink(UUID userId) {
    return issueLink(pending(userId));
  }

  @Override
  @Transactional
  public void cancel(UUID userId) {
    guardianConsents.cancel(pending(userId).cancel(clock.instant()));
  }

  private GuardianConsent pending(UUID userId) {
    return guardianConsents
        .latest(userId)
        .filter(c -> c.status() == GuardianConsent.Status.PENDING)
        .orElseThrow(GuardianConsentNotAllowed::notPending);
  }

  private GuardianLink issueLink(GuardianConsent consent) {
    var token = GuardianLinkToken.generate(randomBytes);
    var linked = consent.withLink(clock.instant().plus(linkTtl));
    guardianConsents.replaceLink(linked, token);
    return new GuardianLink(linkBaseUrl + "#" + token.value(), linked.linkExpiresAt());
  }

  private static GuardianRelationship parseRelationship(String relationship) {
    try {
      return GuardianRelationship.fromCode(relationship);
    } catch (IllegalArgumentException e) {
      throw new InvalidAccountData("relationship-invalid", "Parentesco inválido.");
    }
  }
}
