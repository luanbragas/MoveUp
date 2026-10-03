package br.com.moveup.support;

import static br.com.moveup.support.TestJwt.bearer;
import static br.com.moveup.support.TestJwt.token;
import static br.com.moveup.support.TestJwt.tokenWithEmail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Pessoas de teste pela API: cadastra personal e aluno e chama endpoints como eles. */
public final class ApiActors {

  /** Versão dos textos legais no application.yml. */
  public static final String VERSION = "2026-10-02-rascunho";

  private final MockMvc mvc;

  public ApiActors(MockMvc mvc) {
    this.mvc = mvc;
  }

  /** Personal com organização e termos aceitos; devolve o uid do provedor. */
  public String professional(String name, String business) throws Exception {
    var uid = "uid-" + UUID.randomUUID();
    register(
        uid,
        "{\"name\": \"%s\", \"role\": \"professional\", \"businessName\": \"%s\"}"
            .formatted(name, business));
    consent(uid, "terms", "privacy");
    return uid;
  }

  /** Aluno adulto com os aceites. */
  public String client(String name) throws Exception {
    var uid = "uid-" + UUID.randomUUID();
    register(
        uid,
        "{\"name\": \"%s\", \"role\": \"client\", \"birthDate\": \"%s\"}"
            .formatted(name, LocalDate.now().minusYears(30)));
    consent(uid, "terms", "privacy", "health_data");
    return uid;
  }

  public ResultActions as(String uid, MockHttpServletRequestBuilder request) throws Exception {
    return mvc.perform(request.header(HttpHeaders.AUTHORIZATION, bearer(token(uid))));
  }

  public ResultActions asJson(String uid, MockHttpServletRequestBuilder request, String body)
      throws Exception {
    return as(uid, request.contentType(MediaType.APPLICATION_JSON).content(body));
  }

  public static ResultActions expectProblem(ResultActions result, int status, String code)
      throws Exception {
    return result.andExpect(status().is(status)).andExpect(jsonPath("$.code").value(code));
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
}
