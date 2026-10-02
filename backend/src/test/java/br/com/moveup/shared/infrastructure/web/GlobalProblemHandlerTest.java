package br.com.moveup.shared.infrastructure.web;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.moveup.shared.domain.DomainException;
import br.com.moveup.shared.domain.ResourceNotFound;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Todo {@code code} de erro tem teste (BACKEND-PATTERN, seção 12). */
@WebMvcTest(controllers = GlobalProblemHandlerTest.ProblemController.class)
@AutoConfigureMockMvc(addFilters = false) // segurança tem teste próprio; aqui só o handler
@Import({
  GlobalProblemHandler.class,
  Problems.class,
  TraceIds.class,
  GlobalProblemHandlerTest.ProblemController.class
})
class GlobalProblemHandlerTest {

  static final class RepsRangeInvalid extends DomainException {
    RepsRangeInvalid() {
      super("reps-range-invalid", "A repetição mínima não pode ser maior que a máxima.");
    }
  }

  record Prescription(@NotBlank String name, @Min(1) int reps, @Size(max = 3) String note) {}

  @RestController
  static class ProblemController {

    @GetMapping("/test/domain")
    String domain() {
      throw new RepsRangeInvalid();
    }

    @GetMapping("/test/not-found")
    String notFound() {
      throw new ResourceNotFound();
    }

    @GetMapping("/test/db/{sqlState}")
    String database(@PathVariable String sqlState) {
      throw new DataIntegrityViolationException(
          "falha", new SQLException("Key (email)=(ana@example.test) already exists", sqlState));
    }

    @GetMapping("/test/boom")
    String boom() {
      throw new IllegalStateException("select answers from anamnesis where client_id = ...");
    }

    @PostMapping("/test/prescriptions")
    String create(@Valid @RequestBody Prescription prescription) {
      return "ok";
    }
  }

  @Autowired MockMvc mvc;

  @Test
  void regraDeDominioViolada_422ComCodeDaExcecao() throws Exception {
    expectProblem(mvc.perform(get("/test/domain")), 422, "reps-range-invalid")
        .andExpect(
            jsonPath("$.detail").value("A repetição mínima não pode ser maior que a máxima."))
        .andExpect(jsonPath("$.title").value(Titles.FALLBACK))
        .andExpect(jsonPath("$.instance").value("/test/domain"));
  }

  @Test
  void recursoNaoEncontrado_404() throws Exception {
    expectProblem(mvc.perform(get("/test/not-found")), 404, "resource-not-found")
        .andExpect(jsonPath("$.title").value("Recurso não encontrado"));
  }

  @Test
  void rotaInexistente_404() throws Exception {
    expectProblem(mvc.perform(get("/nao-existe")), 404, "resource-not-found");
  }

  @Test
  void metodoNaoPermitido_405() throws Exception {
    expectProblem(mvc.perform(delete("/test/domain")), 405, "method-not-allowed");
  }

  @ParameterizedTest
  @CsvSource({
    "P0002, 409, invite-expired",
    "P0003, 409, plan-limit-reached",
    "P0004, 409, client-already-linked",
    "P0001, 409, state-conflict",
    "23505, 409, already-exists",
    "23503, 422, invalid-reference",
    "28000, 404, resource-not-found",
    "42501, 404, resource-not-found"
  })
  void erroDoBancoTraduzido(String sqlState, int status, String code) throws Exception {
    expectProblem(mvc.perform(get("/test/db/" + sqlState)), status, code)
        .andExpect(content().string(not(containsString("ana@example.test"))))
        .andExpect(content().string(not(containsString("Key ("))));
  }

  @Test
  void erroDoBancoDesconhecido_500SemVazarAMensagem() throws Exception {
    expectProblem(mvc.perform(get("/test/db/XX000")), 500, "internal-error")
        .andExpect(content().string(not(containsString("ana@example.test"))));
  }

  @Test
  void erroInesperado_500SemStackTraceNemSql() throws Exception {
    expectProblem(mvc.perform(get("/test/boom")), 500, "internal-error")
        .andExpect(jsonPath("$.detail").value("Erro inesperado."))
        .andExpect(content().string(not(containsString("anamnesis"))))
        .andExpect(content().string(not(containsString("IllegalStateException"))));
  }

  @Test
  void validacao_400ComCamposESemOValorEnviado() throws Exception {
    var body = "{\"name\": \"\", \"reps\": 0, \"note\": \"valor-sigiloso\"}";

    expectProblem(
            mvc.perform(
                post("/test/prescriptions").contentType(MediaType.APPLICATION_JSON).content(body)),
            400,
            "validation-failed")
        .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("name", "reps", "note")))
        .andExpect(jsonPath("$.errors[*].code", containsInAnyOrder("not-blank", "min", "size")))
        .andExpect(content().string(not(containsString("valor-sigiloso"))));
  }

  @Test
  void jsonMalformado_400SemEcoarOConteudo() throws Exception {
    var body = "{\"name\": \"dor-no-joelho\", ";

    expectProblem(
            mvc.perform(
                post("/test/prescriptions").contentType(MediaType.APPLICATION_JSON).content(body)),
            400,
            "malformed-request")
        .andExpect(content().string(not(containsString("dor-no-joelho"))));
  }

  @Test
  void formatoNaoSuportado_415() throws Exception {
    expectProblem(
        mvc.perform(post("/test/prescriptions").contentType(MediaType.TEXT_PLAIN).content("x")),
        415,
        "unsupported-media-type");
  }

  private static ResultActions expectProblem(ResultActions result, int status, String code)
      throws Exception {
    return result
        .andExpect(status().is(status))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(status))
        .andExpect(jsonPath("$.code").value(code))
        .andExpect(jsonPath("$.type").value(Problems.TYPE_BASE + code))
        .andExpect(jsonPath("$.traceId", matchesPattern("[0-9a-f]{32}")));
  }
}
