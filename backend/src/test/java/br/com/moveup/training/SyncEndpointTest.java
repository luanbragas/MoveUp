package br.com.moveup.training;

import static br.com.moveup.support.ApiActors.expectProblem;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.moveup.support.ApiActors;
import br.com.moveup.support.TestJwt;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** GET /v1/sync: o planejado do aluno por programa, com cursor e tombstone (Fase 2, F2-5). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestJwt.Keys.class)
class SyncEndpointTest {

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    TestJwt.registerProperties(registry);
  }

  @Autowired MockMvc mvc;

  private static String json(ResultActions result) throws Exception {
    return result.andReturn().getResponse().getContentAsString();
  }

  @Test
  void alunoBaixaOProgramaEDepoisSoOQueMudou() throws Exception {
    var api = new ApiActors(mvc);
    var pro = api.professional("Ana Souza", "Studio Ana");
    var student = api.client("Bia Lima");
    var invite = json(api.asJson(pro, post("/v1/clients"), "{\"name\": \"Bia Lima\"}"));
    String linkId = JsonPath.read(invite, "$.linkId");
    api.as(student, post("/v1/invites/" + JsonPath.read(invite, "$.code") + "/accept"))
        .andExpect(status().isOk());
    String bench =
        JsonPath.read(
            json(api.as(pro, get("/v1/exercises").param("q", "Supino reto com barra"))), "$[0].id");

    // sem programa ainda: nada a baixar
    api.as(student, get("/v1/sync"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.programs", empty()));

    String programId =
        JsonPath.read(
            json(
                api.asJson(
                    pro,
                    post("/v1/coaching-links/" + linkId + "/programs"),
                    "{\"name\": \"Hipertrofia\", \"scheduleMode\": \"fixed_days\"}")),
            "$.id");
    String workoutId =
        JsonPath.read(
            json(
                api.asJson(
                    pro,
                    post("/v1/programs/" + programId + "/workouts"),
                    "{\"name\": \"Treino A\"}")),
            "$.id");
    api.asJson(
            pro,
            put("/v1/workouts/" + workoutId).header(HttpHeaders.IF_MATCH, "\"r1\""),
            """
            {"name": "Treino A", "content": {"blocks": [{"method": "sequential", "exercises": [
              {"exerciseId": "%s", "sets": [{"repsMin": 8, "repsMax": 10, "loadKg": 55}]}]}]}}
            """
                .formatted(bench))
        .andExpect(status().isOk());
    api.asJson(
            pro,
            put("/v1/programs/" + programId).header(HttpHeaders.IF_MATCH, "\"r2\""),
            """
            {"name": "Hipertrofia", "scheduleMode": "fixed_days",
             "schedule": [{"workoutId": "%s", "weekdays": [1, 4]}]}
            """
                .formatted(workoutId))
        .andExpect(status().isOk());

    var first =
        json(
            api.as(student, get("/v1/sync"))
                .andExpect(jsonPath("$.programs", hasSize(1)))
                .andExpect(jsonPath("$.programs[0].deleted").value(false))
                .andExpect(jsonPath("$.programs[0].workouts[0].weekdays", containsInAnyOrder(1, 4)))
                .andExpect(
                    jsonPath(
                            "$.programs[0].workouts[0].content.blocks[0].exercises[0].sets[0].loadKg")
                        .value(55))
                .andExpect(jsonPath("$.exercises[*].name", hasItem("Supino reto com barra"))));
    var cursor = Instant.parse(JsonPath.read(first, "$.cursor"));

    // cursor "do futuro" (sem janela): nada mudou depois dele
    api.as(student, get("/v1/sync").param("since", cursor.plusSeconds(300).toString()))
        .andExpect(jsonPath("$.programs", empty()));

    // programa novo arquiva o anterior: vem o tombstone e o novo
    api.asJson(
            pro,
            post("/v1/coaching-links/" + linkId + "/programs"),
            "{\"name\": \"Força\", \"scheduleMode\": \"sequence\", \"weeklyTarget\": 3}")
        .andExpect(status().isCreated());
    api.as(student, get("/v1/sync").param("since", cursor.toString()))
        .andExpect(jsonPath("$.programs", hasSize(2)))
        .andExpect(jsonPath("$.programs[?(@.id == '" + programId + "')].deleted", hasItem(true)))
        .andExpect(jsonPath("$.programs[?(@.name == 'Força')].deleted", hasItem(false)));
    // a primeira sincronização de um aparelho novo não traz tombstones antigos
    api.as(student, get("/v1/sync")).andExpect(jsonPath("$.programs", hasSize(1)));

    expectProblem(api.as(pro, get("/v1/sync")), 403, "forbidden");
  }
}
