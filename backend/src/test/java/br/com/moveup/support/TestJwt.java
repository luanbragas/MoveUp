package br.com.moveup.support;

import br.com.moveup.db.PostgresTestDatabase;
import br.com.moveup.shared.infrastructure.security.FirebaseJwt;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.function.UnaryOperator;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;

/**
 * Tokens assinados de verdade (RS256) no formato do ID token do Firebase, para os testes de ponta a
 * ponta. Só a fonte das chaves públicas muda (chave de teste em vez do JWKS do Google); a validação
 * é a de produção ({@link FirebaseJwt#validator}).
 *
 * <p>Uso: {@code @Import(TestJwt.Keys.class)} e {@code TestJwt.registerProperties(registry)} no
 * {@code @DynamicPropertySource}.
 */
public final class TestJwt {

  public static final String PROJECT = "moveup-test";
  public static final RSAKey KEY = rsaKey();
  public static final RSAKey OTHER_KEY = rsaKey();

  private TestJwt() {}

  @TestConfiguration
  public static class Keys {
    @Bean
    @Primary
    JwtDecoder testJwtDecoder(Clock clock) throws JOSEException {
      var decoder = NimbusJwtDecoder.withPublicKey(KEY.toRSAPublicKey()).build();
      decoder.setJwtValidator(FirebaseJwt.validator(PROJECT, clock));
      return decoder;
    }
  }

  /** Banco de teste (como {@code app_api}) e o projeto do Firebase dos tokens de teste. */
  public static void registerProperties(DynamicPropertyRegistry registry) {
    PostgresTestDatabase.registerSpringProperties(registry);
    registry.add("moveup.auth.firebase.project-id", () -> PROJECT);
  }

  public static String bearer(String token) {
    return "Bearer " + token;
  }

  public static String token(String uid) {
    return token(uid, UnaryOperator.identity());
  }

  /** Token com o e-mail no claim, como o Firebase emite para Google/Apple/e-mail. */
  public static String tokenWithEmail(String uid, String email) {
    return token(uid, c -> c.claim("email", email).claim("email_verified", true));
  }

  public static String token(String uid, UnaryOperator<JWTClaimsSet.Builder> change) {
    return signed(change.apply(claims(uid)).build(), KEY);
  }

  /** Claims como o Firebase emite um ID token. */
  public static JWTClaimsSet.Builder claims(String uid) {
    var now = Instant.now();
    return new JWTClaimsSet.Builder()
        .issuer(FirebaseJwt.issuer(PROJECT))
        .audience(PROJECT)
        .subject(uid)
        .issueTime(Date.from(now.minusSeconds(10)))
        .expirationTime(Date.from(now.plusSeconds(3600)))
        .claim("auth_time", now.minusSeconds(60).getEpochSecond());
  }

  public static String signed(JWTClaimsSet claims, RSAKey key) {
    try {
      var jwt =
          new SignedJWT(
              new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(), claims);
      jwt.sign(new RSASSASigner(key));
      return jwt.serialize();
    } catch (JOSEException e) {
      throw new IllegalStateException(e);
    }
  }

  private static RSAKey rsaKey() {
    try {
      return new RSAKeyGenerator(2048).keyID(UUID.randomUUID().toString()).generate();
    } catch (JOSEException e) {
      throw new IllegalStateException(e);
    }
  }
}
