package br.com.moveup.accounts;

import static br.com.moveup.db.DbFixtures.exec;
import static br.com.moveup.db.DbFixtures.user;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Ponta a ponta de {@code GET /v1/me}: token assinado de verdade (RS256), validação do Firebase,
 * resolução {@code sub} → {@code app_user} e respostas de erro. Só a fonte das chaves públicas é
 * trocada (chave de teste em vez do JWKS do Google). A aplicação conecta como {@code app_api}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MeEndpointTest {

  private static final String PROJECT = "moveup-test";
  private static final RSAKey KEY = rsaKey();
  private static final RSAKey OTHER_KEY = rsaKey();

  private static UUID professional;
  private static String professionalUid;
  private static String anonymizedUid;

  @TestConfiguration
  static class TestKeys {
    @Bean
    @Primary
    JwtDecoder testJwtDecoder(Clock clock) throws JOSEException {
      var decoder = NimbusJwtDecoder.withPublicKey(KEY.toRSAPublicKey()).build();
      decoder.setJwtValidator(FirebaseJwt.validator(PROJECT, clock));
      return decoder;
    }
  }

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    PostgresTestDatabase.registerSpringProperties(registry);
    registry.add("moveup.auth.firebase.project-id", () -> PROJECT);
  }

  @BeforeAll
  static void seed() throws SQLException {
    PostgresTestDatabase.start(); // @BeforeAll roda antes de o contexto (e o banco) subir
    professional = user();
    professionalUid = "uid-" + professional;
    exec(
        "insert into auth_identity(provider, subject, user_id) values ('firebase', ?, ?)",
        professionalUid,
        professional);
    exec("insert into professional_profile(user_id) values (?)", professional);

    var anonymized = user();
    anonymizedUid = "uid-" + anonymized;
    exec(
        "insert into auth_identity(provider, subject, user_id) values ('firebase', ?, ?)",
        anonymizedUid,
        anonymized);
    exec("update app_user set deleted_at = now(), anonymized_at = now() where id = ?", anonymized);
  }

  @Autowired MockMvc mvc;

  @Test
  void usuarioCadastradoRecebeOsPropriosDados() throws Exception {
    mvc.perform(get("/v1/me").header(HttpHeaders.AUTHORIZATION, bearer(token(professionalUid))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(professional.toString()))
        .andExpect(jsonPath("$.email").value(professional + "@example.test"))
        .andExpect(jsonPath("$.locale").value("pt-BR"))
        .andExpect(jsonPath("$.weightUnit").value("kg"))
        .andExpect(jsonPath("$.professional").value(true));
  }

  @Test
  void loginSemCadastro_404AccountNotRegistered() throws Exception {
    expectProblem(
        mvc.perform(get("/v1/me").header(HttpHeaders.AUTHORIZATION, bearer(token("uid-novo")))),
        404,
        "account-not-registered");
  }

  @Test
  void contaAnonimizada_404AccountNotRegistered() throws Exception {
    expectProblem(
        mvc.perform(get("/v1/me").header(HttpHeaders.AUTHORIZATION, bearer(token(anonymizedUid)))),
        404,
        "account-not-registered");
  }

  @Test
  void semToken_401() throws Exception {
    expectProblem(mvc.perform(get("/v1/me")), 401, "unauthenticated")
        .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, containsString("Bearer")));
  }

  @Test
  void tokensInvalidos_401() throws Exception {
    var invalid =
        new String[] {
          "nao-e-um-jwt",
          token(professionalUid, c -> c.audience("outro-projeto")),
          token(professionalUid, c -> c.issuer(FirebaseJwt.issuer("outro-projeto"))),
          token(
              professionalUid,
              c ->
                  c.issueTime(Date.from(Instant.now().minusSeconds(7200)))
                      .expirationTime(Date.from(Instant.now().minusSeconds(3600)))),
          signed(claims(professionalUid).build(), OTHER_KEY)
        };
    for (var t : invalid) {
      expectProblem(
          mvc.perform(get("/v1/me").header(HttpHeaders.AUTHORIZATION, bearer(t))),
          401,
          "unauthenticated");
    }
  }

  @Test
  void rotaForaDaApiEhNegada_403() throws Exception {
    expectProblem(
        mvc.perform(
            get("/qualquer-coisa")
                .header(HttpHeaders.AUTHORIZATION, bearer(token(professionalUid)))),
        403,
        "forbidden");
  }

  @Test
  void healthNaoPedeToken() throws Exception {
    mvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }

  @Test
  void respostaTemHeadersDeSeguranca() throws Exception {
    mvc.perform(get("/v1/me").header(HttpHeaders.AUTHORIZATION, bearer(token(professionalUid))))
        .andExpect(header().string("X-Content-Type-Options", "nosniff"))
        .andExpect(header().string("X-Frame-Options", "DENY"))
        .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
        .andExpect(header().string("Content-Security-Policy", containsString("default-src 'none'")))
        .andExpect(header().string("Referrer-Policy", "no-referrer"));
  }

  @Test
  void corsFechado() throws Exception {
    mvc.perform(
            options("/v1/me")
                .header(HttpHeaders.ORIGIN, "https://site-qualquer.example")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
        .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
  }

  // ---------------------------------------------------------------------------------------------

  private static ResultActions expectProblem(ResultActions result, int status, String code)
      throws Exception {
    return result
        .andExpect(status().is(status))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value(code))
        .andExpect(jsonPath("$.traceId").isNotEmpty());
  }

  private static String bearer(String token) {
    return "Bearer " + token;
  }

  private static String token(String uid) {
    return token(uid, UnaryOperator.identity());
  }

  private static String token(String uid, UnaryOperator<JWTClaimsSet.Builder> change) {
    return signed(change.apply(claims(uid)).build(), KEY);
  }

  /** Claims como o Firebase emite um ID token. */
  private static JWTClaimsSet.Builder claims(String uid) {
    var now = Instant.now();
    return new JWTClaimsSet.Builder()
        .issuer(FirebaseJwt.issuer(PROJECT))
        .audience(PROJECT)
        .subject(uid)
        .issueTime(Date.from(now.minusSeconds(10)))
        .expirationTime(Date.from(now.plusSeconds(3600)))
        .claim("auth_time", now.minusSeconds(60).getEpochSecond());
  }

  private static String signed(JWTClaimsSet claims, RSAKey key) {
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
