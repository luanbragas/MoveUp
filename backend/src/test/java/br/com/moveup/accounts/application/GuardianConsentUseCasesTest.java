package br.com.moveup.accounts.application;

import static br.com.moveup.accounts.application.AccountUseCasesTest.CLOCK;
import static br.com.moveup.accounts.application.AccountUseCasesTest.ORIGIN;
import static br.com.moveup.accounts.application.AccountUseCasesTest.TODAY;
import static br.com.moveup.accounts.application.AccountUseCasesTest.VERSIONS;
import static br.com.moveup.accounts.application.AccountUseCasesTest.client;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.moveup.accounts.application.port.in.GuardianAuthorization;
import br.com.moveup.accounts.application.port.in.ManageGuardianRequest;
import br.com.moveup.accounts.application.usecase.GetMeUseCase;
import br.com.moveup.accounts.application.usecase.GuardianAuthorizationUseCase;
import br.com.moveup.accounts.application.usecase.ManageGuardianRequestUseCase;
import br.com.moveup.accounts.application.usecase.RegisterAccountUseCase;
import br.com.moveup.accounts.domain.exception.AccountNotRegistered;
import br.com.moveup.accounts.domain.exception.ConsentVersionOutdated;
import br.com.moveup.accounts.domain.exception.GuardianAuthorizationNotFound;
import br.com.moveup.accounts.domain.exception.GuardianConsentNotAllowed;
import br.com.moveup.accounts.fakes.InMemoryAccounts;
import br.com.moveup.accounts.fakes.InMemoryConsents;
import br.com.moveup.shared.domain.DomainException;
import br.com.moveup.shared.domain.IdGenerator;
import java.time.Clock;
import java.time.Duration;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Menor pede, responsável autoriza ou recusa pelo link (LGPD, art. 14; decisão de 03/10/2026). */
class GuardianConsentUseCasesTest {

  final InMemoryAccounts accounts = new InMemoryAccounts();
  final InMemoryConsents consents = new InMemoryConsents();
  final IdGenerator ids = UUID::randomUUID;
  final Random random = new Random(42);

  final RegisterAccountUseCase register =
      new RegisterAccountUseCase(accounts, orgId -> {}, ids, CLOCK);
  final GetMeUseCase getMe = new GetMeUseCase(accounts, consents, consents, () -> VERSIONS, CLOCK);
  final ManageGuardianRequestUseCase requests =
      new ManageGuardianRequestUseCase(
          accounts,
          consents,
          () -> VERSIONS,
          ids,
          random::nextBytes,
          CLOCK,
          Duration.ofDays(7),
          "https://site.test/autorizar/");
  final GuardianAuthorizationUseCase guardian =
      new GuardianAuthorizationUseCase(consents, () -> VERSIONS, CLOCK);

  UUID minor;

  @BeforeEach
  void registerMinor() {
    minor = register.handle(client("uid-teen", "teen@x.test", TODAY.minusYears(15)));
  }

  @Test
  void menorPedeEOResponsavelAutorizaPeloLink() {
    assertThat(getMe.handle(minor).guardianConsentRequired()).isTrue();
    assertThat(getMe.handle(minor).guardianRequest()).isNull();

    var link = requests.request(request(minor));

    assertThat(link.url()).startsWith("https://site.test/autorizar/#");
    assertThat(link.expiresAt()).isEqualTo(CLOCK.instant().plus(Duration.ofDays(7)));
    var pending = getMe.handle(minor);
    assertThat(pending.guardianConsentRequired()).isTrue(); // só vale depois do responsável
    assertThat(pending.guardianRequest().status()).isEqualTo("pending");
    assertThat(pending.guardianRequest().guardianName()).isEqualTo("Maria Souza");

    var preview = guardian.preview(tokenOf(link));
    assertThat(preview.minorFirstName()).isEqualTo("Bia");
    assertThat(preview.relationship()).isEqualTo("mother");
    assertThat(preview.docVersion()).isEqualTo("g-1");

    guardian.decide(decision(link, true, "g-1"));

    var verified = getMe.handle(minor);
    assertThat(verified.guardianConsentRequired()).isFalse();
    assertThat(verified.guardianRequest()).isNull();
    assertThat(consents.lastDecisionOrigin).isEqualTo(ORIGIN);
    // o link vale uma vez, e não cabe um segundo pedido
    assertThatThrownBy(() -> guardian.preview(tokenOf(link)))
        .isInstanceOf(GuardianAuthorizationNotFound.class);
    assertThatThrownBy(() -> requests.request(request(minor)))
        .satisfies(e -> assertThat(codeOf(e)).isEqualTo(GuardianConsentNotAllowed.ALREADY_ACTIVE));
  }

  @Test
  void responsavelRecusaEOMenorPodePedirDeNovo() {
    var link = requests.request(request(minor));

    guardian.decide(decision(link, false, "g-1"));

    var declined = getMe.handle(minor);
    assertThat(declined.guardianConsentRequired()).isTrue();
    assertThat(declined.guardianRequest().status()).isEqualTo("declined");
    assertThat(declined.guardianRequest().linkExpiresAt()).isNull();
    assertThat(requests.request(request(minor)).url()).isNotEqualTo(link.url());
  }

  @Test
  void reenviarInvalidaOLinkAnterior() {
    var first = requests.request(request(minor));

    var second = requests.resendLink(minor);

    assertThat(second.url()).isNotEqualTo(first.url());
    assertThatThrownBy(() -> guardian.preview(tokenOf(first)))
        .isInstanceOf(GuardianAuthorizationNotFound.class);
    assertThat(guardian.preview(tokenOf(second)).guardianName()).isEqualTo("Maria Souza");
  }

  @Test
  void menorCancelaParaIndicarOutraPessoa() {
    var link = requests.request(request(minor));

    requests.cancel(minor);

    assertThat(getMe.handle(minor).guardianRequest()).isNull();
    assertThatThrownBy(() -> guardian.preview(tokenOf(link)))
        .isInstanceOf(GuardianAuthorizationNotFound.class);
    assertThatThrownBy(() -> requests.cancel(minor))
        .satisfies(e -> assertThat(codeOf(e)).isEqualTo(GuardianConsentNotAllowed.NOT_PENDING));
    assertThatThrownBy(() -> requests.resendLink(minor))
        .satisfies(e -> assertThat(codeOf(e)).isEqualTo(GuardianConsentNotAllowed.NOT_PENDING));
    assertThat(requests.request(request(minor)).url()).isNotBlank(); // cabe um novo pedido
  }

  @Test
  void linkVencidoOuInventadoNaoServe() {
    var link = requests.request(request(minor));
    var later =
        new GuardianAuthorizationUseCase(
            consents, () -> VERSIONS, Clock.offset(CLOCK, Duration.ofDays(8)));

    assertThatThrownBy(() -> later.preview(tokenOf(link)))
        .isInstanceOf(GuardianAuthorizationNotFound.class);
    assertThatThrownBy(() -> later.decide(decision(link, true, "g-1")))
        .isInstanceOf(GuardianAuthorizationNotFound.class);
    assertThatThrownBy(() -> guardian.preview("nao-e-um-segredo"))
        .isInstanceOf(GuardianAuthorizationNotFound.class);
    assertThat(getMe.handle(minor).guardianConsentRequired()).isTrue();
  }

  @Test
  void responsavelPrecisaAceitarOTermoVigente() {
    var link = requests.request(request(minor));

    assertThatThrownBy(() -> guardian.decide(decision(link, true, "g-0")))
        .isInstanceOf(ConsentVersionOutdated.class);
    assertThat(getMe.handle(minor).guardianConsentRequired()).isTrue();
  }

  @Test
  void adultoOuContaInexistenteNaoPedem() {
    var adult = register.handle(client("uid-adult", "adult@x.test", TODAY.minusYears(30)));

    assertThatThrownBy(() -> requests.request(request(adult)))
        .satisfies(e -> assertThat(codeOf(e)).isEqualTo(GuardianConsentNotAllowed.NOT_REQUIRED));
    assertThatThrownBy(() -> requests.request(request(UUID.randomUUID())))
        .isInstanceOf(AccountNotRegistered.class);
  }

  // ---------------------------------------------------------------------------------------------

  static ManageGuardianRequest.RequestCommand request(UUID userId) {
    return new ManageGuardianRequest.RequestCommand(userId, "Maria Souza", "mother", "g-1", ORIGIN);
  }

  static GuardianAuthorization.Decision decision(
      ManageGuardianRequest.GuardianLink link, boolean approve, String version) {
    return new GuardianAuthorization.Decision(tokenOf(link), approve, version, ORIGIN);
  }

  static String tokenOf(ManageGuardianRequest.GuardianLink link) {
    return link.url().substring(link.url().indexOf('#') + 1);
  }

  static String codeOf(Throwable e) {
    return ((DomainException) e).code();
  }
}
