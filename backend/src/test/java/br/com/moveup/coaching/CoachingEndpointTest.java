package br.com.moveup.coaching;

import static br.com.moveup.support.TestJwt.bearer;
import static br.com.moveup.support.TestJwt.token;
import static br.com.moveup.support.TestJwt.tokenWithEmail;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.moveup.support.TestJwt;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
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

/** Convite e vínculo de ponta a ponta pela API (Fase 1, F1-2). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestJwt.Keys.class)
class CoachingEndpointTest {

  static final String VERSION = "2026-10-02-rascunho";

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    TestJwt.registerProperties(registry);
  }

  @Autowired MockMvc mvc;

  @Test
  void conviteAceiteEVidaDoVinculo() throws Exception {
    var pro = professional();
    var student = client(LocalDate.now().minusYears(30), true);

    var invitation = invite(pro, "Bia Souza");
    var code = JsonPath.<String>read(invitation, "$.code");
    var linkId = JsonPath.<String>read(invitation, "$.linkId");

    // o aluno vê de quem é o convite e aceita
    as(student, get("/v1/invites/" + code))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.professionalName").value("Carlos Lima"))
        .andExpect(jsonPath("$.organizationName").value("Studio Fit"));
    as(student, post("/v1/invites/" + code.toLowerCase() + "/accept"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.linkId").value(linkId));
    as(student, get("/v1/me/coaching-links"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].linkId").value(linkId))
        .andExpect(jsonPath("$[0].status").value("active"))
        .andExpect(jsonPath("$[0].professionalName").value("Carlos Lima"))
        .andExpect(jsonPath("$[0].organizationName").value("Studio Fit"));

    // o profissional vê o aluno ativo; o convite não vale mais
    as(pro, get("/v1/clients"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].linkId").value(linkId))
        .andExpect(jsonPath("$.items[0].status").value("active"))
        .andExpect(jsonPath("$.items[0].pendingInvite").doesNotExist());
    expectProblem(as(student, get("/v1/invites/" + code)), 409, "invite-expired");

    // inativa, reativa e o aluno encerra o vínculo dele
    as(pro, post("/v1/coaching-links/" + linkId + "/inactivate")).andExpect(status().isNoContent());
    as(pro, post("/v1/coaching-links/" + linkId + "/reactivate")).andExpect(status().isNoContent());
    as(student, post("/v1/me/coaching-links/" + linkId + "/end")).andExpect(status().isNoContent());
    as(pro, get("/v1/clients")).andExpect(jsonPath("$.items[*].linkId", not(hasItem(linkId))));
    expectProblem(
        as(student, post("/v1/me/coaching-links/" + linkId + "/end")), 404, "resource-not-found");
  }

  @Test
  void soAlunoComCadastroCompletoAceita() throws Exception {
    var pro = professional();
    var code = JsonPath.<String>read(invite(pro, "Bia"), "$.code");
    var withoutConsents = client(LocalDate.now().minusYears(30), false);
    var minorWithoutGuardian = client(LocalDate.now().minusYears(15), true);

    expectProblem(
        as(withoutConsents, post("/v1/invites/" + code + "/accept")), 409, "onboarding-incomplete");
    expectProblem(
        as(minorWithoutGuardian, post("/v1/invites/" + code + "/accept")),
        409,
        "onboarding-incomplete");
    expectProblem(
        as(professional(), post("/v1/invites/" + code + "/accept")),
        409,
        "invite-for-clients-only");
  }

  @Test
  void reenviarInvalidaOCodigoAnterior() throws Exception {
    var pro = professional();
    var student = client(LocalDate.now().minusYears(30), true);
    var first = invite(pro, "Bia");
    var linkId = JsonPath.<String>read(first, "$.linkId");
    var oldCode = JsonPath.<String>read(first, "$.code");

    var newCode =
        JsonPath.<String>read(
            as(pro, post("/v1/clients/" + linkId + "/invite"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.code");

    expectProblem(as(student, get("/v1/invites/" + oldCode)), 409, "invite-expired");
    as(student, get("/v1/invites/" + newCode)).andExpect(status().isOk());

    as(pro, delete("/v1/clients/" + linkId + "/invite")).andExpect(status().isNoContent());
    expectProblem(as(student, get("/v1/invites/" + newCode)), 409, "invite-expired");
  }

  @Test
  void outroProfissionalNaoVeNemMexe() throws Exception {
    var pro = professional();
    var stranger = professional();
    var invitation = invite(pro, "Bia");
    var linkId = JsonPath.<String>read(invitation, "$.linkId");

    as(stranger, get("/v1/clients"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[*].linkId", not(hasItem(linkId))));
    for (var path :
        new String[] {
          "/v1/coaching-links/" + linkId + "/end",
          "/v1/coaching-links/" + linkId + "/inactivate",
          "/v1/clients/" + linkId + "/invite"
        }) {
      expectProblem(as(stranger, post(path)), 404, "resource-not-found");
    }
  }

  @Test
  void alunoNaoConvida() throws Exception {
    var student = client(LocalDate.now().minusYears(30), true);

    expectProblem(
        mvc.perform(
            post("/v1/clients")
                .header(HttpHeaders.AUTHORIZATION, bearer(token(student)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Outro Aluno\"}")),
        403,
        "forbidden");
  }

  // ---------------------------------------------------------------- apoio

  /** Profissional cadastrado com termos aceitos; devolve o uid do provedor. */
  private String professional() throws Exception {
    var uid = "uid-" + UUID.randomUUID();
    register(
        uid,
        "{\"name\": \"Carlos Lima\", \"role\": \"professional\", \"businessName\": \"Studio Fit\"}");
    consent(uid, "terms", "privacy");
    return uid;
  }

  /** Aluno cadastrado, opcionalmente com os aceites; devolve o uid do provedor. */
  private String client(LocalDate birthDate, boolean withConsents) throws Exception {
    var uid = "uid-" + UUID.randomUUID();
    register(
        uid,
        "{\"name\": \"Bia Souza\", \"role\": \"client\", \"birthDate\": \"" + birthDate + "\"}");
    if (withConsents) {
      consent(uid, "terms", "privacy", "health_data");
    }
    return uid;
  }

  private void register(String uid, String body) throws Exception {
    mvc.perform(
            post("/v1/accounts")
                .header(
                    HttpHeaders.AUTHORIZATION, bearer(tokenWithEmail(uid, uid + "@example.test")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isCreated());
  }

  private void consent(String uid, String... kinds) throws Exception {
    var grants = new StringBuilder();
    for (var kind : kinds) {
      if (!grants.isEmpty()) {
        grants.append(',');
      }
      grants.append("{\"kind\": \"%s\", \"docVersion\": \"%s\"}".formatted(kind, VERSION));
    }
    mvc.perform(
            post("/v1/consents")
                .header(HttpHeaders.AUTHORIZATION, bearer(token(uid)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"grants\": [" + grants + "]}"))
        .andExpect(status().isNoContent());
  }

  private String invite(String proUid, String name) throws Exception {
    return mvc.perform(
            post("/v1/clients")
                .header(HttpHeaders.AUTHORIZATION, bearer(token(proUid)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"" + name + "\", \"phone\": \"+55 11 98765-4321\"}"))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  private ResultActions as(
      String uid,
      org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
      throws Exception {
    return mvc.perform(request.header(HttpHeaders.AUTHORIZATION, bearer(token(uid))));
  }

  private static ResultActions expectProblem(ResultActions result, int status, String code)
      throws Exception {
    return result.andExpect(status().is(status)).andExpect(jsonPath("$.code").value(code));
  }
}
