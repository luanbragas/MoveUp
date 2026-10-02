package br.com.moveup.shared.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class FirebaseJwtTest {

  private static final String PROJECT = "moveup-test";
  private static final Instant NOW = Instant.parse("2026-10-02T12:00:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

  @Test
  void tokenValidoDoProjetoPassa() {
    assertThat(errorsOf(b -> b)).isEmpty();
  }

  @Test
  void issuerDeOutroProjetoFalha() {
    assertThat(errorsOf(b -> b.issuer(FirebaseJwt.issuer("outro-projeto")))).isNotEmpty();
  }

  @Test
  void audienceDeOutroProjetoFalha() {
    assertThat(errorsOf(b -> b.audience(List.of("outro-projeto")))).isNotEmpty();
  }

  @Test
  void subVazioFalha() {
    assertThat(errorsOf(b -> b.subject(" "))).isNotEmpty();
  }

  @Test
  void tokenExpiradoFalha() {
    assertThat(errorsOf(b -> b.issuedAt(NOW.minusSeconds(7200)).expiresAt(NOW.minusSeconds(3600))))
        .isNotEmpty();
  }

  @Test
  void iatNoFuturoFalha() {
    assertThat(errorsOf(b -> b.issuedAt(NOW.plusSeconds(600)).expiresAt(NOW.plusSeconds(4200))))
        .isNotEmpty();
  }

  @Test
  void authTimeAusenteOuNoFuturoFalha() {
    assertThat(errorsOf(b -> b.claims(c -> c.remove("auth_time")))).isNotEmpty();
    assertThat(errorsOf(b -> b.claim("auth_time", NOW.plusSeconds(600).getEpochSecond())))
        .isNotEmpty();
  }

  @Test
  void projetoNaoConfiguradoImpedeASubida() {
    assertThatThrownBy(() -> FirebaseJwt.validator(" ", CLOCK))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private static List<?> errorsOf(UnaryOperator<Jwt.Builder> change) {
    var builder =
        Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .issuer(FirebaseJwt.issuer(PROJECT))
            .audience(List.of(PROJECT))
            .subject("firebase-uid-123")
            .issuedAt(NOW.minusSeconds(60))
            .expiresAt(NOW.plusSeconds(3540))
            .claim("auth_time", NOW.minusSeconds(120).getEpochSecond());
    var jwt = change.apply(builder).build();
    return List.copyOf(FirebaseJwt.validator(PROJECT, CLOCK).validate(jwt).getErrors());
  }
}
