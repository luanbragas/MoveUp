package br.com.moveup.shared.infrastructure.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Validação do ID token do Firebase Auth, conforme a documentação do Firebase para verificar tokens
 * em servidor de terceiros: assinatura RS256 pelas chaves públicas (JWKS) do Google, {@code exp},
 * {@code iat} e {@code auth_time} no passado, {@code aud} = id do projeto, {@code iss} = {@code
 * https://securetoken.google.com/<projeto>} e {@code sub} não vazio (é o uid do Firebase).
 */
public final class FirebaseJwt {

  /** Valor de {@code auth_identity.provider}: o uid do Firebase é o mesmo para Google, Apple... */
  public static final String PROVIDER = "firebase";

  static final String JWK_SET_URI =
      "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com";

  private static final Duration CLOCK_SKEW = Duration.ofSeconds(60);

  private FirebaseJwt() {}

  public static String issuer(String projectId) {
    return "https://securetoken.google.com/" + projectId;
  }

  /** Decoder de produção: busca as chaves públicas sob demanda (não na subida) e faz cache. */
  public static JwtDecoder decoder(String projectId, Clock clock) {
    var decoder =
        NimbusJwtDecoder.withJwkSetUri(JWK_SET_URI).jwsAlgorithm(SignatureAlgorithm.RS256).build();
    decoder.setJwtValidator(validator(projectId, clock));
    return decoder;
  }

  public static OAuth2TokenValidator<Jwt> validator(String projectId, Clock clock) {
    if (projectId == null || projectId.isBlank()) {
      throw new IllegalArgumentException("id do projeto do Firebase não configurado");
    }
    var timestamps = new JwtTimestampValidator(CLOCK_SKEW);
    timestamps.setClock(clock);
    return new DelegatingOAuth2TokenValidator<>(
        timestamps,
        new JwtIssuerValidator(issuer(projectId)),
        check(
            jwt -> jwt.getAudience() != null && jwt.getAudience().contains(projectId),
            "aud inválido"),
        check(jwt -> jwt.getSubject() != null && !jwt.getSubject().isBlank(), "sub ausente"),
        check(jwt -> notInFuture(jwt.getIssuedAt(), clock), "iat no futuro"),
        check(jwt -> notInFuture(instant(jwt.getClaim("auth_time")), clock), "auth_time inválido"));
  }

  private static boolean notInFuture(Instant instant, Clock clock) {
    return instant != null && !instant.isAfter(clock.instant().plus(CLOCK_SKEW));
  }

  private static Instant instant(Object claim) {
    return switch (claim) {
      case Instant i -> i;
      case Date d -> d.toInstant();
      case Number n -> Instant.ofEpochSecond(n.longValue());
      case null, default -> null;
    };
  }

  private static OAuth2TokenValidator<Jwt> check(
      java.util.function.Predicate<Jwt> condition, String description) {
    var error = new OAuth2Error("invalid_token", description, null);
    return jwt ->
        condition.test(jwt)
            ? OAuth2TokenValidatorResult.success()
            : OAuth2TokenValidatorResult.failure(error);
  }
}
