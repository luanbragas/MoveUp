package br.com.moveup.accounts;

import static br.com.moveup.db.DbFixtures.single;
import static br.com.moveup.support.TestJwt.bearer;
import static br.com.moveup.support.TestJwt.token;
import static br.com.moveup.support.TestJwt.tokenWithEmail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.moveup.support.TestJwt;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Cadastro, consentimentos e responsável pelo menor, de ponta a ponta (Fase 1, F1-1). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestJwt.Keys.class)
class AccountsEndpointTest {

  static final String VERSION = "2026-10-02-rascunho"; // application.yml
  static final LocalDate TODAY = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    TestJwt.registerProperties(registry);
  }

  @Autowired MockMvc mvc;

  @Test
  void profissionalSeCadastraGanhaOrganizacaoTesteEAceitaOsTermos() throws Exception {
    var uid = "uid-" + UUID.randomUUID();
    var email = uid + "@example.test";

    var id =
        register(
                uid,
                email,
                """
                {"name": "Carlos Lima", "role": "professional", "businessName": "Studio Fit",
                 "registryNumber": "012345-G/SP"}
                """)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.role").value("professional"))
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.missingConsents", contains("privacy", "terms")))
            .andReturn()
            .getResponse()
            .getContentAsString()
            .replaceAll(".*\"id\":\"([0-9a-f-]+)\".*", "$1");
    var userId = UUID.fromString(id);

    // organização com o profissional como dono e assinatura de teste
    assertThat(single("select name from organization where owner_user_id = ?", userId))
        .isEqualTo("Studio Fit");
    assertThat(
            single(
                "select p.code || ':' || s.status from subscription s join plan p on p.id = s.plan_id"
                    + " join organization o on o.id = s.organization_id where o.owner_user_id = ?",
                userId))
        .isEqualTo("trial:trialing");
    assertThat(single("select registry_number from professional_profile where user_id = ?", userId))
        .isEqualTo("012345-G/SP");

    // mesmo login de novo: 409
    expectProblem(
        register(uid, email, "{\"name\": \"Carlos\", \"role\": \"professional\"}"),
        409,
        "account-already-registered");

    // aceita termos e privacidade (versão vigente) e o onboarding fecha
    mvc.perform(
            post("/v1/consents")
                .header(HttpHeaders.AUTHORIZATION, bearer(token(uid)))
                .header(HttpHeaders.USER_AGENT, "MoveUp/1.0 (teste)")
                .contentType(MediaType.APPLICATION_JSON)
                .content(grants("terms", "privacy")))
        .andExpect(status().isNoContent());
    me(uid).andExpect(jsonPath("$.missingConsents", empty()));
    assertThat(
            single(
                "select host(ip) || ' ' || user_agent from consent where user_id = ? and kind ="
                    + " 'terms'",
                userId))
        .isEqualTo("127.0.0.1 MoveUp/1.0 (teste)");

    // revogar volta a pedir
    mvc.perform(delete("/v1/consents/terms").header(HttpHeaders.AUTHORIZATION, bearer(token(uid))))
        .andExpect(status().isNoContent());
    me(uid).andExpect(jsonPath("$.missingConsents", contains("terms")));
  }

  @Test
  void aceiteDeVersaoAntigaEhRecusado() throws Exception {
    var uid = registerClient(TODAY.minusYears(30));

    expectProblem(
        mvc.perform(
            post("/v1/consents")
                .header(HttpHeaders.AUTHORIZATION, bearer(token(uid)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"grants\": [{\"kind\": \"terms\", \"docVersion\": \"2020-01-01\"}]}")),
        409,
        "consent-version-outdated");
  }

  @Test
  void alunoMenorSoCompletaComOResponsavel() throws Exception {
    var uid = registerClient(TODAY.minusYears(15));
    me(uid)
        .andExpect(jsonPath("$.role").value("client"))
        .andExpect(jsonPath("$.minor").value(true))
        .andExpect(jsonPath("$.guardianConsentRequired").value(true))
        .andExpect(jsonPath("$.missingConsents", contains("health_data", "privacy", "terms")));

    guardian(uid).andExpect(status().isNoContent());

    me(uid).andExpect(jsonPath("$.guardianConsentRequired").value(false));
    expectProblem(guardian(uid), 409, "guardian-consent-already-active");
  }

  @Test
  void adultoNaoRegistraResponsavel() throws Exception {
    var uid = registerClient(TODAY.minusYears(25));

    expectProblem(guardian(uid), 409, "guardian-consent-not-required");
  }

  @Test
  void versoesVigentesDosTextos() throws Exception {
    var uid = registerClient(TODAY.minusYears(25));

    mvc.perform(get("/v1/legal-documents").header(HttpHeaders.AUTHORIZATION, bearer(token(uid))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.consents.terms").value(VERSION))
        .andExpect(jsonPath("$.consents.health_data").value(VERSION))
        .andExpect(jsonPath("$.guardianConsent").value(VERSION));
  }

  @Test
  void cadastroInvalidoTemCodeProprio() throws Exception {
    var uid = "uid-" + UUID.randomUUID();
    // token sem e-mail (ex.: login por telefone)
    expectProblem(
        mvc.perform(
            post("/v1/accounts")
                .header(HttpHeaders.AUTHORIZATION, bearer(token(uid)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"name\": \"Ana\", \"role\": \"client\", \"birthDate\": \"2000-01-01\"}")),
        422,
        "email-required");
    expectProblem(
        register(uid, uid + "@example.test", "{\"name\": \"Ana\", \"role\": \"client\"}"),
        422,
        "birth-date-required");
    expectProblem(
        register(uid, uid + "@example.test", "{\"name\": \"Ana\", \"role\": \"admin\"}"),
        400,
        "validation-failed");
  }

  @Test
  void semCadastroNaoHaConsentimento() throws Exception {
    expectProblem(
        mvc.perform(
            post("/v1/consents")
                .header(HttpHeaders.AUTHORIZATION, bearer(token("uid-" + UUID.randomUUID())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(grants("terms"))),
        404,
        "account-not-registered");
  }

  // ---------------------------------------------------------------------------------------------

  private String registerClient(LocalDate birthDate) throws Exception {
    var uid = "uid-" + UUID.randomUUID();
    register(
            uid,
            uid + "@example.test",
            "{\"name\": \"Bia Souza\", \"role\": \"client\", \"birthDate\": \"" + birthDate + "\"}")
        .andExpect(status().isCreated());
    return uid;
  }

  private ResultActions register(String uid, String email, String body) throws Exception {
    return mvc.perform(
        post("/v1/accounts")
            .header(HttpHeaders.AUTHORIZATION, bearer(tokenWithEmail(uid, email)))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }

  private ResultActions guardian(String uid) throws Exception {
    return mvc.perform(
        post("/v1/guardian-consent")
            .header(HttpHeaders.AUTHORIZATION, bearer(token(uid)))
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {"guardianName": "Maria Souza", "guardianEmail": "maria.souza@example.test",
                 "relationship": "mother", "docVersion": "%s"}
                """
                    .formatted(VERSION)));
  }

  private ResultActions me(String uid) throws Exception {
    return mvc.perform(get("/v1/me").header(HttpHeaders.AUTHORIZATION, bearer(token(uid))))
        .andExpect(status().isOk());
  }

  private static String grants(String... kinds) {
    var items = new StringBuilder();
    for (var kind : kinds) {
      if (!items.isEmpty()) {
        items.append(',');
      }
      items.append("{\"kind\": \"%s\", \"docVersion\": \"%s\"}".formatted(kind, VERSION));
    }
    return "{\"grants\": [" + items + "]}";
  }

  private static ResultActions expectProblem(ResultActions result, int status, String code)
      throws Exception {
    return result.andExpect(status().is(status)).andExpect(jsonPath("$.code").value(code));
  }
}
