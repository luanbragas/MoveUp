package br.com.moveup.training;

import static br.com.moveup.support.ApiActors.expectProblem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.moveup.support.ApiActors;
import br.com.moveup.support.TestJwt;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** Modelos de treino, edição com If-Match e regras de bloco pela API (Fase 2, F2-2). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestJwt.Keys.class)
class WorkoutsEndpointTest {

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    TestJwt.registerProperties(registry);
  }

  @Autowired MockMvc mvc;
  ApiActors api;

  @BeforeEach
  void setUp() {
    api = new ApiActors(mvc);
  }

  private String exerciseId(String pro, String name) throws Exception {
    return JsonPath.read(
        api.as(pro, get("/v1/exercises").param("q", name))
            .andReturn()
            .getResponse()
            .getContentAsString(),
        "$[0].id");
  }

  private String body(String name, String bench, String fly, String squat) {
    return """
        {"name": "%s", "content": {"goal": "Peito e pernas", "estimatedMinutes": 55, "blocks": [
          {"method": "sequential", "exercises": [
            {"exerciseId": "%s", "restSeconds": 90, "notes": "Pegada média",
             "sets": [{"type": "warmup", "repsMin": 12, "loadKg": 30},
                      {"repsMin": 8, "repsMax": 10, "loadKg": 55, "targetRir": 2},
                      {"repsMin": 8, "repsMax": 10, "loadKg": 55, "targetRir": 2}]}]},
          {"name": "Biset", "method": "superset", "exercises": [
            {"exerciseId": "%s", "sets": [{"repsMin": 12, "loadKg": 14}]},
            {"exerciseId": "%s", "sets": [{"repsMin": 12}]}]},
          {"name": "Finalizador", "method": "hiit", "preset": "tabata", "exercises": [
            {"exerciseId": "%s"}]}
        ]}}
        """
        .formatted(name, bench, fly, squat, squat);
  }

  @Test
  void criaLeEditaComIfMatch() throws Exception {
    var pro = api.professional("Ana Souza", "Studio Ana");
    var bench = exerciseId(pro, "Supino reto com barra");
    var fly = exerciseId(pro, "Crucifixo com halteres");
    var squat = exerciseId(pro, "Agachamento livre");

    var created =
        api.asJson(pro, post("/v1/workout-templates"), body("Treino A", bench, fly, squat))
            .andExpect(status().isCreated())
            .andExpect(header().string(HttpHeaders.ETAG, "\"r1\""))
            .andExpect(jsonPath("$.template").value(true))
            .andExpect(
                jsonPath("$.content.blocks[0].exercises[0].exerciseName")
                    .value("Supino reto com barra"))
            .andExpect(jsonPath("$.content.blocks[0].exercises[0].sets[1].repsMax").value(10))
            .andExpect(jsonPath("$.content.blocks[0].exercises[0].sets[0].type").value("warmup"))
            .andExpect(jsonPath("$.content.blocks[2].rounds").value(8))
            .andExpect(jsonPath("$.content.blocks[2].workSeconds").value(20))
            .andReturn()
            .getResponse()
            .getContentAsString();
    var id = JsonPath.<String>read(created, "$.id");

    api.as(pro, get("/v1/workouts/" + id))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ETAG, "\"r1\""))
        .andExpect(jsonPath("$.content.blocks[1].exercises[0].exerciseId").value(fly));
    api.as(pro, get("/v1/workout-templates"))
        .andExpect(jsonPath("$[0].id").value(id))
        .andExpect(jsonPath("$[0].blocks").value(3))
        .andExpect(jsonPath("$[0].exercises").value(4));

    // salva com a revisão certa; a antiga dá 412 e sem If-Match dá 428
    api.asJson(
            pro,
            put("/v1/workouts/" + id).header(HttpHeaders.IF_MATCH, "\"r1\""),
            body("Treino A · peito", bench, fly, squat))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ETAG, "\"r2\""))
        .andExpect(jsonPath("$.name").value("Treino A · peito"))
        .andExpect(jsonPath("$.versionNumber").value(1));
    expectProblem(
        api.asJson(
            pro,
            put("/v1/workouts/" + id).header(HttpHeaders.IF_MATCH, "\"r1\""),
            body("Outro aparelho", bench, fly, squat)),
        412,
        "version-mismatch");
    expectProblem(
        api.asJson(pro, put("/v1/workouts/" + id), body("Sem versão", bench, fly, squat)),
        428,
        "if-match-required");

    api.as(pro, delete("/v1/workouts/" + id).header(HttpHeaders.IF_MATCH, "\"r2\""))
        .andExpect(status().isNoContent());
    expectProblem(api.as(pro, get("/v1/workouts/" + id)), 404, "resource-not-found");
    api.as(pro, get("/v1/workout-templates")).andExpect(jsonPath("$[*].id", not(hasItem(id))));
  }

  @Test
  void outraOrganizacaoNaoVeNemUsaExercicioAlheio() throws Exception {
    var ana = api.professional("Ana Souza", "Studio Ana");
    var carlos = api.professional("Carlos Lima", "Studio Fit");
    var bench = exerciseId(ana, "Supino reto com barra");
    var fly = exerciseId(ana, "Crucifixo com halteres");
    var squat = exerciseId(ana, "Agachamento livre");
    var id =
        JsonPath.<String>read(
            api.asJson(ana, post("/v1/workout-templates"), body("Treino A", bench, fly, squat))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.id");
    var carlosOwn =
        JsonPath.<String>read(
            api.asJson(
                    carlos,
                    post("/v1/exercises"),
                    "{\"name\": \"Remada cavalinho\", \"modality\": \"strength\","
                        + " \"trackingType\": \"reps_load\"}")
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.id");

    expectProblem(api.as(carlos, get("/v1/workouts/" + id)), 404, "resource-not-found");
    expectProblem(
        api.asJson(
            carlos,
            put("/v1/workouts/" + id).header(HttpHeaders.IF_MATCH, "\"r1\""),
            body("X", bench, fly, squat)),
        404,
        "resource-not-found");
    // Ana não usa o exercício próprio do Carlos
    expectProblem(
        api.asJson(ana, post("/v1/workout-templates"), body("Treino B", carlosOwn, fly, squat)),
        422,
        "exercise-unknown");
  }

  @Test
  void regrasDoBlocoVoltamComCodigo() throws Exception {
    var pro = api.professional("Ana Souza", "Studio Ana");
    var bench = exerciseId(pro, "Supino reto com barra");

    expectProblem(
        api.asJson(
            pro,
            post("/v1/workout-templates"),
            """
            {"name": "Biset sozinho", "content": {"blocks": [
              {"method": "superset", "exercises": [{"exerciseId": "%s", "sets": [{"repsMin": 10}]}]}
            ]}}
            """
                .formatted(bench)),
        422,
        "superset-needs-two");
    expectProblem(
        api.asJson(
            pro,
            post("/v1/workout-templates"),
            """
            {"name": "Faixa invertida", "content": {"blocks": [
              {"method": "sequential", "exercises": [
                {"exerciseId": "%s", "sets": [{"repsMin": 12, "repsMax": 8}]}]}
            ]}}
            """
                .formatted(bench)),
        422,
        "reps-invalid");
    var empty =
        api.asJson(
                pro,
                post("/v1/workout-templates"),
                "{\"name\": \"Rascunho\", \"content\": {\"blocks\": []}}")
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(JsonPath.<Integer>read(empty, "$.revision")).isEqualTo(1);
    assertThat(empty).doesNotContain("null"); // modelo vazio: sem goal, notes, programId…
  }
}
