package br.com.moveup.coaching;

import static br.com.moveup.db.DbFixtures.exec;
import static br.com.moveup.support.TestJwt.bearer;
import static br.com.moveup.support.TestJwt.token;
import static br.com.moveup.support.TestJwt.tokenWithEmail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.moveup.support.TestJwt;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * "Pronto quando" da Fase 1 (PLANO.md): dois profissionais e três alunos mostram isolamento
 * completo (RLS + autorização no caso de uso), e o limite do plano barra o aceite excedente,
 * inclusive com aceites simultâneos. Tudo pela API, conectando ao banco como {@code app_api}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestJwt.Keys.class)
class PhaseOneIsolationTest {

  static final String VERSION = "2026-10-02-rascunho";

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    TestJwt.registerProperties(registry);
  }

  @Autowired MockMvc mvc;

  @Test
  void doisProfissionaisTresAlunosIsoladosELimiteDoPlano() throws Exception {
    // dois profissionais, cada um com plano de 1 vaga
    var proA = professional("Ana Souza");
    var proB = professional("Bruno Lima");
    limitPlan(proA, 1);
    limitPlan(proB, 1);
    // três alunos com cadastro completo
    var c1 = client();
    var c2 = client();
    var c3 = client();

    // A convida c1 e c2; c1 entra, c2 esbarra no limite
    var a1 = invite(proA, "Aluno Um");
    var a2 = invite(proA, "Aluno Dois");
    var linkA1 = accept(c1, code(a1)).andExpect(status().isOk());
    expectProblem(accept(c2, code(a2)), 409, "plan-limit-reached");

    // B convida c2 e c3 e os dois aceitam AO MESMO TEMPO: exatamente um passa
    var b2 = invite(proB, "Aluno Dois");
    var b3 = invite(proB, "Aluno Tres");
    var outcomes = acceptConcurrently(List.of(c2, c3), List.of(code(b2), code(b3)));
    assertThat(outcomes).containsExactlyInAnyOrder("200", "409:plan-limit-reached");

    // isolamento das listas: cada profissional só vê os próprios vínculos
    var aLinks = List.of(linkId(a1), linkId(a2));
    var bLinks = List.of(linkId(b2), linkId(b3));
    as(proA, get("/v1/clients"))
        .andExpect(jsonPath("$.items[*].linkId", containsInAnyOrder(aLinks.toArray())));
    as(proB, get("/v1/clients"))
        .andExpect(jsonPath("$.items[*].linkId", containsInAnyOrder(bLinks.toArray())));

    // nenhum profissional mexe no vínculo do outro (404, não revela que existe)
    for (var foreign : bLinks) {
      expectProblem(
          as(proA, post("/v1/coaching-links/" + foreign + "/end")), 404, "resource-not-found");
      expectProblem(
          as(proA, post("/v1/clients/" + foreign + "/invite")), 404, "resource-not-found");
    }
    var linkC1 = JsonPath.<String>read(contentOf(linkA1), "$.linkId");
    expectProblem(
        as(proB, post("/v1/coaching-links/" + linkC1 + "/inactivate")), 404, "resource-not-found");

    // aluno não encerra vínculo de outro aluno
    for (var foreign : bLinks) {
      expectProblem(
          as(c1, post("/v1/me/coaching-links/" + foreign + "/end")), 404, "resource-not-found");
    }

    // aluno com personal ativo não aceita outro convite
    // (B sem vaga nem convida: libera a vaga antes, para o motivo da recusa ser o vínculo do c1)
    expectProblem(
        mvc.perform(
            post("/v1/clients")
                .header(HttpHeaders.AUTHORIZATION, bearer(token(proB)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Aluno Um\"}")),
        409,
        "plan-limit-reached");
    as(proB, post("/v1/coaching-links/" + activeLinkOf(proB) + "/inactivate"))
        .andExpect(status().isNoContent());
    var anotherFromB = invite(proB, "Aluno Um");
    expectProblem(accept(c1, code(anotherFromB)), 409, "client-already-linked");

    // o vínculo do c1 com A continua intacto
    as(proA, get("/v1/clients"))
        .andExpect(jsonPath("$.items[?(@.linkId == '" + linkC1 + "')].status").value("active"));
  }

  // ---------------------------------------------------------------- apoio

  private String professional(String name) throws Exception {
    var uid = "uid-" + UUID.randomUUID();
    register(uid, "{\"name\": \"" + name + "\", \"role\": \"professional\"}");
    consent(uid, "terms", "privacy");
    return uid;
  }

  private String client() throws Exception {
    var uid = "uid-" + UUID.randomUUID();
    register(
        uid,
        "{\"name\": \"Aluno\", \"role\": \"client\", \"birthDate\": \""
            + LocalDate.now().minusYears(30)
            + "\"}");
    consent(uid, "terms", "privacy", "health_data");
    return uid;
  }

  /** Troca o plano da organização do profissional por um com o limite dado. */
  private void limitPlan(String professionalUid, int maxActiveClients) throws Exception {
    var plan = UUID.randomUUID();
    exec(
        "insert into plan(id, code, name, max_active_clients, price_cents, billing_interval)"
            + " values (?, ?, 'Teste', ?, 0, 'month')",
        plan,
        "test-" + plan,
        maxActiveClients);
    exec(
        "update subscription set plan_id = ? where organization_id = (select o.id from"
            + " organization o join auth_identity a on a.user_id = o.owner_user_id where"
            + " a.subject = ?)",
        plan,
        professionalUid);
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
    var grants = new ArrayList<String>();
    for (var kind : kinds) {
      grants.add("{\"kind\": \"%s\", \"docVersion\": \"%s\"}".formatted(kind, VERSION));
    }
    mvc.perform(
            post("/v1/consents")
                .header(HttpHeaders.AUTHORIZATION, bearer(token(uid)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"grants\": [" + String.join(",", grants) + "]}"))
        .andExpect(status().isNoContent());
  }

  private String invite(String proUid, String name) throws Exception {
    return contentOf(
        mvc.perform(
                post("/v1/clients")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token(proUid)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\": \"" + name + "\"}"))
            .andExpect(status().isCreated()));
  }

  private ResultActions accept(String clientUid, String code) throws Exception {
    return as(clientUid, post("/v1/invites/" + code + "/accept"));
  }

  private List<String> acceptConcurrently(List<String> clients, List<String> codes)
      throws Exception {
    var start = new CountDownLatch(1);
    var tasks = new ArrayList<Callable<String>>();
    for (int i = 0; i < clients.size(); i++) {
      var clientUid = clients.get(i);
      var code = codes.get(i);
      tasks.add(
          () -> {
            start.await();
            var response = accept(clientUid, code).andReturn().getResponse();
            return response.getStatus() == 200
                ? "200"
                : response.getStatus()
                    + ":"
                    + JsonPath.read(response.getContentAsString(), "$.code");
          });
    }
    var outcomes = new ArrayList<String>();
    try (var pool = Executors.newFixedThreadPool(tasks.size())) {
      var futures = tasks.stream().map(pool::submit).toList();
      start.countDown();
      for (var future : futures) {
        outcomes.add(future.get());
      }
    }
    return outcomes;
  }

  private String activeLinkOf(String proUid) throws Exception {
    List<String> active =
        JsonPath.read(
            contentOf(as(proUid, get("/v1/clients"))), "$.items[?(@.status == 'active')].linkId");
    return active.getFirst();
  }

  private ResultActions as(String uid, MockHttpServletRequestBuilder request) throws Exception {
    return mvc.perform(request.header(HttpHeaders.AUTHORIZATION, bearer(token(uid))));
  }

  private static String code(String invitation) {
    return JsonPath.read(invitation, "$.code");
  }

  private static String linkId(String invitation) {
    return JsonPath.read(invitation, "$.linkId");
  }

  private static String contentOf(ResultActions result) throws Exception {
    return result.andReturn().getResponse().getContentAsString();
  }

  private static ResultActions expectProblem(ResultActions result, int status, String code)
      throws Exception {
    return result.andExpect(status().is(status)).andExpect(jsonPath("$.code").value(code));
  }
}
