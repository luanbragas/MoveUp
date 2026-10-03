package br.com.moveup.accounts.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.moveup.accounts.application.port.in.ManageConsents;
import br.com.moveup.accounts.application.port.in.RegisterAccount;
import br.com.moveup.accounts.application.port.in.RequestOrigin;
import br.com.moveup.accounts.application.usecase.GetMeUseCase;
import br.com.moveup.accounts.application.usecase.ManageConsentsUseCase;
import br.com.moveup.accounts.application.usecase.RegisterAccountUseCase;
import br.com.moveup.accounts.domain.exception.AccountAlreadyRegistered;
import br.com.moveup.accounts.domain.exception.AccountNotRegistered;
import br.com.moveup.accounts.domain.exception.ConsentVersionOutdated;
import br.com.moveup.accounts.domain.model.ConsentKind;
import br.com.moveup.accounts.domain.model.LegalVersions;
import br.com.moveup.accounts.domain.model.LoginIdentity;
import br.com.moveup.accounts.fakes.InMemoryAccounts;
import br.com.moveup.accounts.fakes.InMemoryConsents;
import br.com.moveup.billing.api.StartTrial;
import br.com.moveup.shared.domain.DomainException;
import br.com.moveup.shared.domain.IdGenerator;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Casos de uso de contas com fakes em memória (BACKEND-PATTERN, seção 12). */
class AccountUseCasesTest {

  // 02/10/2026 09:00 em Brasília
  static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-02T12:00:00Z"), ZoneOffset.UTC);
  static final LocalDate TODAY = LocalDate.of(2026, 10, 2);
  static final RequestOrigin ORIGIN = new RequestOrigin("203.0.113.7", "MoveUp/1.0");
  static final LegalVersions VERSIONS =
      new LegalVersions(
          Map.of(
              ConsentKind.TERMS, "t-1",
              ConsentKind.PRIVACY, "p-1",
              ConsentKind.HEALTH_DATA, "h-1",
              ConsentKind.PHOTOS, "f-1"),
          "g-1");

  final InMemoryAccounts accounts = new InMemoryAccounts();
  final InMemoryConsents consents = new InMemoryConsents();
  final List<UUID> trials = new ArrayList<>();
  final StartTrial startTrial = trials::add;
  final IdGenerator ids = UUID::randomUUID;

  final RegisterAccountUseCase register =
      new RegisterAccountUseCase(accounts, startTrial, ids, CLOCK);
  final GetMeUseCase getMe = new GetMeUseCase(accounts, consents, consents, () -> VERSIONS, CLOCK);
  final ManageConsentsUseCase manageConsents = new ManageConsentsUseCase(consents, () -> VERSIONS);

  @Test
  void profissionalGanhaOrganizacaoEPeriodoDeTeste() {
    var id = register.handle(professional("uid-pro", "pro@x.test"));

    var orgId = accounts.organizationsByOwner.get(id);
    assertThat(orgId).isNotNull();
    assertThat(trials).containsExactly(orgId);
    var me = getMe.handle(id);
    assertThat(me.role()).isEqualTo("professional");
    assertThat(me.missingConsents()).containsExactly("privacy", "terms");
    assertThat(me.guardianConsentRequired()).isFalse();
  }

  @Test
  void alunoNaoGanhaOrganizacaoEPrecisaDoConsentimentoDeSaude() {
    var id = register.handle(client("uid-aluno", "aluno@x.test", TODAY.minusYears(30)));

    assertThat(accounts.organizationsByOwner).isEmpty();
    assertThat(trials).isEmpty();
    assertThat(getMe.handle(id).missingConsents())
        .containsExactly("health_data", "privacy", "terms");
  }

  @Test
  void loginOuEmailRepetidoNaoCriaSegundaConta() {
    register.handle(professional("uid-1", "pro@x.test"));

    assertThatThrownBy(() -> register.handle(professional("uid-1", "outro@x.test")))
        .isInstanceOf(AccountAlreadyRegistered.class);
    assertThatThrownBy(() -> register.handle(client("uid-2", "PRO@x.test", TODAY.minusYears(20))))
        .isInstanceOf(AccountAlreadyRegistered.class);
  }

  @Test
  void cadastroSemEmailDoProvedorOuComPapelInvalidoEhRejeitado() {
    assertThatThrownBy(() -> register.handle(professional("uid-1", null)))
        .satisfies(e -> assertThat(((DomainException) e).code()).isEqualTo("email-required"));
    var badRole =
        new RegisterAccount.Command(
            new LoginIdentity("firebase", "uid-2"), "x@x.test", "Ana", "admin", null, null, null);
    assertThatThrownBy(() -> register.handle(badRole))
        .satisfies(e -> assertThat(((DomainException) e).code()).isEqualTo("role-invalid"));
    assertThatThrownBy(() -> register.handle(client("uid-3", "c@x.test", null)))
        .satisfies(e -> assertThat(((DomainException) e).code()).isEqualTo("birth-date-required"));
  }

  @Test
  void aceitesCompletamOOnboardingDoAdulto() {
    var id = register.handle(client("uid-aluno", "aluno@x.test", TODAY.minusYears(30)));

    manageConsents.grant(
        id,
        List.of(
            new ManageConsents.Grant("terms", "t-1"),
            new ManageConsents.Grant("privacy", "p-1"),
            new ManageConsents.Grant("health_data", "h-1")),
        ORIGIN);

    assertThat(getMe.handle(id).missingConsents()).isEmpty();
    assertThat(consents.lastOrigin).isEqualTo(ORIGIN);
  }

  @Test
  void aceiteDeVersaoAntigaOuTipoDesconhecidoEhRejeitado() {
    var id = UUID.randomUUID();

    assertThatThrownBy(
            () ->
                manageConsents.grant(id, List.of(new ManageConsents.Grant("terms", "t-0")), ORIGIN))
        .isInstanceOf(ConsentVersionOutdated.class);
    assertThatThrownBy(
            () -> manageConsents.grant(id, List.of(new ManageConsents.Grant("x", "1")), ORIGIN))
        .satisfies(e -> assertThat(((DomainException) e).code()).isEqualTo("consent-kind-invalid"));
    assertThatThrownBy(() -> manageConsents.grant(id, List.of(), ORIGIN))
        .satisfies(e -> assertThat(((DomainException) e).code()).isEqualTo("consents-empty"));
  }

  @Test
  void revogarFazOAceiteVoltarAFaltar() {
    var id = register.handle(professional("uid-pro", "pro@x.test"));
    manageConsents.grant(
        id,
        List.of(
            new ManageConsents.Grant("terms", "t-1"), new ManageConsents.Grant("privacy", "p-1")),
        ORIGIN);

    manageConsents.revoke(id, "privacy", ORIGIN);

    assertThat(getMe.handle(id).missingConsents()).containsExactly("privacy");
  }

  @Test
  void contaInexistenteNaoTemMe() {
    assertThatThrownBy(() -> getMe.handle(UUID.randomUUID()))
        .isInstanceOf(AccountNotRegistered.class);
  }

  @Test
  void versoesVigentesSaemPorCodigo() {
    var view = manageConsents.currentVersions();

    assertThat(view.consents()).containsEntry("health_data", "h-1").hasSize(4);
    assertThat(view.guardianConsent()).isEqualTo("g-1");
  }

  // ---------------------------------------------------------------------------------------------

  static RegisterAccount.Command professional(String uid, String email) {
    return new RegisterAccount.Command(
        new LoginIdentity("firebase", uid), email, "Carlos Lima", "professional", null, "", null);
  }

  static RegisterAccount.Command client(String uid, String email, LocalDate birthDate) {
    return new RegisterAccount.Command(
        new LoginIdentity("firebase", uid), email, "Bia Souza", "client", birthDate, null, null);
  }
}
