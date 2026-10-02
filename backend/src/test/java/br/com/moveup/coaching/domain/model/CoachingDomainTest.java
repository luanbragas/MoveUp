package br.com.moveup.coaching.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.moveup.coaching.domain.exception.CoachingConflict;
import br.com.moveup.coaching.domain.exception.InvalidClientData;
import br.com.moveup.shared.domain.DomainException;
import java.time.Instant;
import java.util.HashSet;
import java.util.OptionalInt;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CoachingDomainTest {

  static final Instant NOW = Instant.parse("2026-10-02T12:00:00Z");
  static final UUID PRO = UUID.randomUUID();

  static CoachingLink link(LinkStatus status) {
    return new CoachingLink(
        UUID.randomUUID(), UUID.randomUUID(), PRO, UUID.randomUUID(), status, null);
  }

  static String codeOf(Throwable error) {
    return ((DomainException) error).code();
  }

  @Nested
  class Lifecycle {

    @Test
    void ativoInativaEReativaComVaga() {
      var link = link(LinkStatus.ACTIVE);

      link.inactivate();
      assertThat(link.status()).isEqualTo(LinkStatus.INACTIVE);

      link.reactivate(2, OptionalInt.of(3));
      assertThat(link.status()).isEqualTo(LinkStatus.ACTIVE);
    }

    @Test
    void reativarSemVagaOuSemAssinaturaEhBarrado() {
      assertThatThrownBy(() -> link(LinkStatus.INACTIVE).reactivate(3, OptionalInt.of(3)))
          .satisfies(e -> assertThat(codeOf(e)).isEqualTo(CoachingConflict.PLAN_LIMIT_REACHED));
      assertThatThrownBy(() -> link(LinkStatus.INACTIVE).reactivate(0, OptionalInt.empty()))
          .satisfies(e -> assertThat(codeOf(e)).isEqualTo(CoachingConflict.PLAN_LIMIT_REACHED));
    }

    @Test
    void transicoesForaDeOrdemSaoBarradas() {
      assertThatThrownBy(() -> link(LinkStatus.PENDING).inactivate())
          .satisfies(e -> assertThat(codeOf(e)).isEqualTo(CoachingConflict.LINK_STATE_INVALID));
      assertThatThrownBy(() -> link(LinkStatus.ACTIVE).reactivate(0, OptionalInt.of(5)))
          .satisfies(e -> assertThat(codeOf(e)).isEqualTo(CoachingConflict.LINK_STATE_INVALID));
      assertThatThrownBy(() -> link(LinkStatus.ACTIVE).requirePending())
          .satisfies(e -> assertThat(codeOf(e)).isEqualTo(CoachingConflict.LINK_STATE_INVALID));
    }

    @Test
    void encerrarGuardaADataEUmaVezSo() {
      var link = link(LinkStatus.INACTIVE);

      link.end(NOW);

      assertThat(link.status()).isEqualTo(LinkStatus.ENDED);
      assertThat(link.endedAt()).contains(NOW);
      assertThatThrownBy(() -> link.end(NOW)).isInstanceOf(CoachingConflict.class);
    }

    @Test
    void sabeDeQuemEh() {
      assertThat(link(LinkStatus.ACTIVE).belongsTo(PRO)).isTrue();
      assertThat(link(LinkStatus.ACTIVE).belongsTo(UUID.randomUUID())).isFalse();
    }

    @Test
    void statusVoltaDoBanco() {
      for (var status : LinkStatus.values()) {
        assertThat(LinkStatus.fromCode(status.code())).isEqualTo(status);
      }
      assertThatThrownBy(() -> LinkStatus.fromCode("x"))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  class Invites {

    @Test
    void codigoUsaSoCaracteresSemAmbiguidade() {
      var codes = new HashSet<String>();
      var random = new java.util.Random(42);
      for (int i = 0; i < 500; i++) {
        var code = InviteCode.generate(random::nextInt);
        assertThat(code.value()).hasSize(8).doesNotContain("0", "O", "1", "I", "L");
        codes.add(code.value());
      }
      assertThat(codes).hasSize(500);
    }

    @Test
    void codigoDigitadoAceitaMinusculasEEspacos() {
      assertThat(InviteCode.parse(" k7m2qx9p ")).contains(new InviteCode("K7M2QX9P"));
      assertThat(InviteCode.parse("K7M2QX9")).isEmpty();
      assertThat(InviteCode.parse("K7M2QX9O")).isEmpty(); // O não existe no alfabeto
      assertThat(InviteCode.parse(null)).isEmpty();
    }
  }

  @Nested
  class PreRegistration {

    @Test
    void normalizaOsCamposOpcionais() {
      var client =
          ClientPreRegistration.of("  Bia   Souza ", " Bia@X.test ", "+55 (11) 98765-4321", " ");

      assertThat(client.name()).isEqualTo("Bia Souza");
      assertThat(client.email()).contains("bia@x.test");
      assertThat(client.phone()).contains("5511987654321");
      assertThat(client.goal()).isEmpty();
    }

    @Test
    void camposInvalidosTemCodeProprio() {
      assertThatThrownBy(() -> ClientPreRegistration.of("B", null, null, null))
          .isInstanceOf(InvalidClientData.class)
          .satisfies(e -> assertThat(codeOf(e)).isEqualTo("name-invalid"));
      assertThatThrownBy(() -> ClientPreRegistration.of("Bia", "sem-arroba", null, null))
          .satisfies(e -> assertThat(codeOf(e)).isEqualTo("email-invalid"));
      assertThatThrownBy(() -> ClientPreRegistration.of("Bia", null, "123", null))
          .satisfies(e -> assertThat(codeOf(e)).isEqualTo("phone-invalid"));
      assertThatThrownBy(() -> ClientPreRegistration.of("Bia", null, null, "x".repeat(501)))
          .satisfies(e -> assertThat(codeOf(e)).isEqualTo("goal-invalid"));
    }
  }
}
