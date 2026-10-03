package br.com.moveup.accounts.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.moveup.accounts.domain.exception.ConsentVersionOutdated;
import br.com.moveup.accounts.domain.exception.GuardianAuthorizationNotFound;
import br.com.moveup.accounts.domain.exception.GuardianConsentNotAllowed;
import br.com.moveup.accounts.domain.exception.InvalidAccountData;
import br.com.moveup.shared.domain.DomainException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AccountDomainTest {

  static final LocalDate TODAY = LocalDate.of(2026, 10, 2);
  static final Instant NOW = Instant.parse("2026-10-02T12:00:00Z");
  static final LoginIdentity IDENTITY = new LoginIdentity("firebase", "uid-1");
  static final LegalVersions VERSIONS =
      new LegalVersions(
          Map.of(
              ConsentKind.TERMS, "t-2",
              ConsentKind.PRIVACY, "p-2",
              ConsentKind.HEALTH_DATA, "h-1",
              ConsentKind.PHOTOS, "f-1"),
          "g-1");

  static String codeOf(Throwable error) {
    return ((DomainException) error).code();
  }

  @Nested
  class ValueObjects {

    @Test
    void emailENomeSaoNormalizados() {
      assertThat(Email.of("  Ana@Example.TEST ").value()).isEqualTo("ana@example.test");
      assertThat(PersonName.of("  Ana   Souza ").value()).isEqualTo("Ana Souza");
    }

    @Test
    void emailENomeInvalidosTemCodeProprio() {
      assertThatThrownBy(() -> Email.of("sem-arroba"))
          .satisfies(e -> assertThat(codeOf(e)).isEqualTo("email-invalid"));
      assertThatThrownBy(() -> Email.of(null)).isInstanceOf(InvalidAccountData.class);
      assertThatThrownBy(() -> PersonName.of("A"))
          .satisfies(e -> assertThat(codeOf(e)).isEqualTo("name-invalid"));
    }

    @Test
    void dataDeNascimentoDecideAMaioridadeNoDiaDoAniversario() {
      var turns18Today = BirthDate.of(TODAY.minusYears(18), TODAY);
      var turns18Tomorrow = BirthDate.of(TODAY.minusYears(18).plusDays(1), TODAY);

      assertThat(turns18Today.isMinorOn(TODAY)).isFalse();
      assertThat(turns18Tomorrow.isMinorOn(TODAY)).isTrue();
      assertThat(turns18Tomorrow.ageOn(TODAY)).isEqualTo(17);
    }

    @Test
    void dataDeNascimentoAusenteFuturaOuAbsurdaEhRejeitada() {
      assertThatThrownBy(() -> BirthDate.of(null, TODAY))
          .satisfies(e -> assertThat(codeOf(e)).isEqualTo("birth-date-required"));
      assertThatThrownBy(() -> BirthDate.of(TODAY.plusDays(1), TODAY))
          .satisfies(e -> assertThat(codeOf(e)).isEqualTo("birth-date-invalid"));
      assertThatThrownBy(() -> BirthDate.of(TODAY.minusYears(130), TODAY))
          .satisfies(e -> assertThat(codeOf(e)).isEqualTo("birth-date-invalid"));
      assertThatThrownBy(() -> BirthDate.of(TODAY.minusYears(2), TODAY))
          .satisfies(e -> assertThat(codeOf(e)).isEqualTo("birth-date-invalid"));
    }
  }

  @Nested
  class Registration {

    @Test
    void alunoNasceComPapelEDataDeNascimento() {
      var birth = BirthDate.of(TODAY.minusYears(15), TODAY);

      var account =
          Account.registerClient(
              UUID.randomUUID(), IDENTITY, PersonName.of("Bia"), Email.of("bia@x.test"), birth);

      assertThat(account.role()).isEqualTo(AccountRole.CLIENT);
      assertThat(account.birthDate()).contains(birth);
      assertThat(account.professional()).isEmpty();
    }

    @Test
    void profissionalUsaONomeComoNegocioQuandoNaoInforma() {
      var name = PersonName.of("Carlos Lima");

      var account =
          Account.registerProfessional(
              UUID.randomUUID(),
              IDENTITY,
              name,
              Email.of("carlos@x.test"),
              ProfessionalSetup.of("  ", " CREF 012345-G/SP ", name));

      assertThat(account.role()).isEqualTo(AccountRole.PROFESSIONAL);
      assertThat(account.professional().orElseThrow().businessName()).isEqualTo("Carlos Lima");
      assertThat(account.professional().orElseThrow().registryNumber())
          .contains("CREF 012345-G/SP");
      assertThat(account.birthDate()).isEmpty();
    }

    @Test
    void profissionalMenorDeIdadeEhRejeitado() {
      var name = PersonName.of("Joao");
      var setup = ProfessionalSetup.of("Studio", null, name);
      var minor = BirthDate.of(TODAY.minusYears(17), TODAY);

      assertThatThrownBy(
              () ->
                  Account.registerProfessional(
                      UUID.randomUUID(), IDENTITY, name, Email.of("j@x.test"), setup, minor, TODAY))
          .satisfies(e -> assertThat(codeOf(e)).isEqualTo("professional-must-be-adult"));
    }

    @Test
    void negocioECrefComTamanhoInvalidoSaoRejeitados() {
      var name = PersonName.of("Carlos");
      assertThatThrownBy(() -> ProfessionalSetup.of("x".repeat(121), null, name))
          .satisfies(e -> assertThat(codeOf(e)).isEqualTo("business-name-invalid"));
      assertThatThrownBy(() -> ProfessionalSetup.of(null, "9".repeat(31), name))
          .satisfies(e -> assertThat(codeOf(e)).isEqualTo("registry-number-invalid"));
    }
  }

  @Nested
  class Onboarding {

    @Test
    void alunoMaiorSemAceitesPrecisaDeTermosPrivacidadeESaude() {
      var profile = client(TODAY.minusYears(30));

      var onboarding = profile.onboardingOn(TODAY, Map.of(), false, VERSIONS);

      assertThat(onboarding.missingConsents())
          .containsExactlyInAnyOrder(
              ConsentKind.TERMS, ConsentKind.PRIVACY, ConsentKind.HEALTH_DATA);
      assertThat(onboarding.minor()).isFalse();
      assertThat(onboarding.guardianConsentRequired()).isFalse();
      assertThat(onboarding.complete()).isFalse();
    }

    @Test
    void aceiteDeVersaoAntigaContaComoPendente() {
      var profile = professional();
      var accepted = new EnumMap<ConsentKind, String>(ConsentKind.class);
      accepted.put(ConsentKind.TERMS, "t-1"); // versão anterior
      accepted.put(ConsentKind.PRIVACY, "p-2");

      var onboarding = profile.onboardingOn(TODAY, accepted, false, VERSIONS);

      assertThat(onboarding.missingConsents()).containsExactly(ConsentKind.TERMS);
    }

    @Test
    void menorSoCompletaComConsentimentoDoResponsavel() {
      var profile = client(TODAY.minusYears(14));
      var accepted =
          Map.of(
              ConsentKind.TERMS, "t-2", ConsentKind.PRIVACY, "p-2", ConsentKind.HEALTH_DATA, "h-1");

      assertThat(profile.onboardingOn(TODAY, accepted, false, VERSIONS).guardianConsentRequired())
          .isTrue();
      assertThat(profile.onboardingOn(TODAY, accepted, true, VERSIONS).complete()).isTrue();
    }

    @Test
    void contaSemPapelNaoTemConsentimentoObrigatorio() {
      var profile = new AccountProfile(UUID.randomUUID(), null, Email.of("x@x.test"), null);

      assertThat(profile.onboardingOn(TODAY, Map.of(), false, VERSIONS).missingConsents())
          .isEmpty();
    }
  }

  @Nested
  class Consents {

    @Test
    void aceiteSoDaVersaoVigente() {
      assertThat(ConsentGrant.accept(ConsentKind.PHOTOS, "f-1", VERSIONS).docVersion())
          .isEqualTo("f-1");
      assertThatThrownBy(() -> ConsentGrant.accept(ConsentKind.PHOTOS, "f-0", VERSIONS))
          .isInstanceOf(ConsentVersionOutdated.class);
      assertThatThrownBy(() -> ConsentGrant.accept(ConsentKind.PHOTOS, null, VERSIONS))
          .isInstanceOf(ConsentVersionOutdated.class);
    }

    @Test
    void versoesVigentesPrecisamCobrirTodosOsTipos() {
      assertThatThrownBy(() -> new LegalVersions(Map.of(ConsentKind.TERMS, "t"), "g"))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new LegalVersions(VERSIONS.consents(), " "))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void menorPedeEOPedidoSoValeComOResponsavel() {
      var minor = client(TODAY.minusYears(16));

      var requested = request(minor, "g-1");

      assertThat(requested.userId()).isEqualTo(minor.userId());
      assertThat(requested.status()).isEqualTo(GuardianConsent.Status.PENDING);
      assertThat(requested.linkOpenAt(NOW)).isFalse(); // sem link ainda
      var linked = requested.withLink(NOW.plus(Duration.ofDays(7)));
      assertThat(linked.linkOpenAt(NOW)).isTrue();
      var approved = linked.approve("g-1", VERSIONS, NOW.plusSeconds(60));
      assertThat(approved.status()).isEqualTo(GuardianConsent.Status.VERIFIED);
      assertThat(approved.isOpen()).isTrue();
      assertThat(approved.linkExpiresAt()).isNull();
    }

    @Test
    void pedidoSoParaMenorComTextoVigente() {
      assertThatThrownBy(() -> request(client(TODAY.minusYears(20)), "g-1"))
          .satisfies(e -> assertThat(codeOf(e)).isEqualTo(GuardianConsentNotAllowed.NOT_REQUIRED));
      assertThatThrownBy(() -> request(client(TODAY.minusYears(16)), "g-0"))
          .isInstanceOf(ConsentVersionOutdated.class);
    }

    @Test
    void recusaEncerraOPedidoECancelarSoComPedidoAberto() {
      var linked = request(client(TODAY.minusYears(16)), "g-1").withLink(NOW.plusSeconds(600));

      var declined = linked.decline(NOW);

      assertThat(declined.status()).isEqualTo(GuardianConsent.Status.DECLINED);
      assertThat(declined.isOpen()).isFalse();
      assertThatThrownBy(() -> declined.cancel(NOW))
          .satisfies(e -> assertThat(codeOf(e)).isEqualTo(GuardianConsentNotAllowed.NOT_PENDING));
      assertThatThrownBy(() -> declined.approve("g-1", VERSIONS, NOW))
          .isInstanceOf(GuardianAuthorizationNotFound.class);
      var cancelled = linked.cancel(NOW);
      assertThat(cancelled.status()).isEqualTo(GuardianConsent.Status.CANCELLED);
      assertThat(cancelled.isOpen()).isFalse();
    }

    @Test
    void linkVencidoNaoDecide() {
      var linked = request(client(TODAY.minusYears(16)), "g-1").withLink(NOW.plusSeconds(60));

      assertThatThrownBy(() -> linked.approve("g-1", VERSIONS, NOW.plusSeconds(60)))
          .isInstanceOf(GuardianAuthorizationNotFound.class);
      assertThatThrownBy(() -> linked.decline(NOW.plusSeconds(61)))
          .isInstanceOf(GuardianAuthorizationNotFound.class);
    }

    @Test
    void segredoDoLinkTemFormatoFixoENaoApareceEmLog() {
      var token = GuardianLinkToken.generate(bytes -> java.util.Arrays.fill(bytes, (byte) 7));

      assertThat(token.value()).hasSize(43);
      assertThat(token.hash()).hasSize(32);
      assertThat(token.toString()).doesNotContain(token.value());
      assertThat(GuardianLinkToken.parse(token.value())).contains(token);
      assertThat(GuardianLinkToken.parse("curto")).isEmpty();
      assertThat(GuardianLinkToken.parse(null)).isEmpty();
    }

    @Test
    void codigosDosEnumsVoltamDoBanco() {
      for (var kind : ConsentKind.values()) {
        assertThat(ConsentKind.fromCode(kind.code())).isEqualTo(kind);
      }
      for (var role : AccountRole.values()) {
        assertThat(AccountRole.fromCode(role.code())).isEqualTo(role);
      }
      for (var relationship : GuardianRelationship.values()) {
        assertThat(GuardianRelationship.fromCode(relationship.code())).isEqualTo(relationship);
      }
      assertThatThrownBy(() -> ConsentKind.fromCode("x"))
          .isInstanceOf(IllegalArgumentException.class);
    }

    private GuardianConsent request(AccountProfile minor, String version) {
      return GuardianConsent.request(
          UUID.randomUUID(),
          minor,
          PersonName.of("Maria"),
          GuardianRelationship.MOTHER,
          version,
          VERSIONS,
          NOW,
          TODAY);
    }
  }

  static AccountProfile client(LocalDate birth) {
    return new AccountProfile(
        UUID.randomUUID(),
        AccountRole.CLIENT,
        Email.of("aluno@x.test"),
        BirthDate.of(birth, TODAY));
  }

  static AccountProfile professional() {
    return new AccountProfile(
        UUID.randomUUID(), AccountRole.PROFESSIONAL, Email.of("pro@x.test"), null);
  }
}
