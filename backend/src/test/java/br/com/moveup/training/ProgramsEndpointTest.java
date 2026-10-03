package br.com.moveup.training;

import static br.com.moveup.db.DbFixtures.exec;
import static br.com.moveup.db.DbFixtures.single;
import static br.com.moveup.support.ApiActors.expectProblem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.moveup.support.ApiActors;
import br.com.moveup.support.TestJwt;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
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

/** Programa, agenda e versões do treino com sessão (Fase 2, F2-3 — critério de pronto). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestJwt.Keys.class)
class ProgramsEndpointTest {

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    TestJwt.registerProperties(registry);
  }

  @Autowired MockMvc mvc;
  ApiActors api;
  String pro;
  String linkId;
  String bench;
  String squat;

  @BeforeEach
  void setUp() throws Exception {
    api = new ApiActors(mvc);
    pro = api.professional("Ana Souza", "Studio Ana");
    var student = api.client("Bia Lima");
    var invite =
        api.asJson(pro, post("/v1/clients"), "{\"name\": \"Bia Lima\"}")
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    linkId = JsonPath.read(invite, "$.linkId");
    api.as(student, post("/v1/invites/" + JsonPath.read(invite, "$.code") + "/accept"))
        .andExpect(status().isOk());
    bench = exercise("Supino reto com barra");
    squat = exercise("Agachamento livre");
  }

  private String exercise(String name) throws Exception {
    return JsonPath.read(
        api.as(pro, get("/v1/exercises").param("q", name))
            .andReturn()
            .getResponse()
            .getContentAsString(),
        "$[0].id");
  }

  private String json(org.springframework.test.web.servlet.ResultActions result) throws Exception {
    return result.andReturn().getResponse().getContentAsString();
  }

  private String content(String exerciseId, String kg) {
    return """
        {"name": "Treino A", "content": {"estimatedMinutes": 50, "blocks": [
          {"method": "sequential", "exercises": [
            {"exerciseId": "%s", "sets": [{"repsMin": 8, "repsMax": 10, "loadKg": %s}]}]}]}}
        """
        .formatted(exerciseId, kg);
  }

  @Test
  void programaComModeloEAgendaDeDiasFixos() throws Exception {
    var template =
        JsonPath.<String>read(
            json(
                api.asJson(pro, post("/v1/workout-templates"), content(squat, "80"))
                    .andExpect(status().isCreated())),
            "$.id");

    api.as(pro, get("/v1/coaching-links/" + linkId + "/programs/active"))
        .andExpect(status().isNoContent());
    var program =
        json(
            api.asJson(
                    pro,
                    post("/v1/coaching-links/" + linkId + "/programs"),
                    """
                    {"name": "Hipertrofia", "goal": "Ganhar massa", "startsOn": "2026-10-05",
                     "endsOn": "2026-11-30", "scheduleMode": "fixed_days"}
                    """)
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.ETAG, "\"r1\"")));
    var programId = JsonPath.<String>read(program, "$.id");

    var a =
        JsonPath.<String>read(
            json(
                api.asJson(
                        pro,
                        post("/v1/programs/" + programId + "/workouts"),
                        "{\"templateId\": \"" + template + "\", \"name\": \"Treino A\"}")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.sourceTemplateId").value(template))
                    .andExpect(jsonPath("$.template").value(false))
                    .andExpect(
                        jsonPath("$.content.blocks[0].exercises[0].exerciseId").value(squat))),
            "$.id");
    var b =
        JsonPath.<String>read(
            json(
                api.asJson(
                        pro,
                        post("/v1/programs/" + programId + "/workouts"),
                        "{\"name\": \"Treino B\"}")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.content.blocks", empty()))),
            "$.id");

    // B antes de A; A na segunda e quinta, B na terça e sexta (cada treino adicionado subiu a
    // revisão)
    api.asJson(
            pro,
            put("/v1/programs/" + programId).header(HttpHeaders.IF_MATCH, "\"r3\""),
            """
            {"name": "Hipertrofia", "scheduleMode": "fixed_days", "schedule": [
              {"workoutId": "%s", "weekdays": [2, 5]},
              {"workoutId": "%s", "weekdays": [1, 4]}]}
            """
                .formatted(b, a))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ETAG, "\"r4\""))
        .andExpect(jsonPath("$.workouts[0].id").value(b))
        .andExpect(jsonPath("$.workouts[0].position").value(1))
        .andExpect(jsonPath("$.workouts[1].weekdays", containsInAnyOrder(1, 4)))
        .andExpect(jsonPath("$.workouts[1].exercises").value(1));

    api.as(pro, get("/v1/coaching-links/" + linkId + "/programs/active"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(programId))
        .andExpect(jsonPath("$.workouts[1].name").value("Treino A"));

    // sequência pede meta semanal; agenda sem todos os treinos não vale
    expectProblem(
        api.asJson(
            pro,
            put("/v1/programs/" + programId).header(HttpHeaders.IF_MATCH, "\"r4\""),
            """
            {"name": "H", "scheduleMode": "sequence",
             "schedule": [{"workoutId": "%s"}, {"workoutId": "%s"}]}
            """
                .formatted(a, b)),
        422,
        "weekly-target-invalid");
    expectProblem(
        api.asJson(
            pro,
            put("/v1/programs/" + programId).header(HttpHeaders.IF_MATCH, "\"r4\""),
            """
            {"name": "H", "scheduleMode": "sequence", "weeklyTarget": 4,
             "schedule": [{"workoutId": "%s"}]}
            """
                .formatted(a)),
        422,
        "schedule-workouts-invalid");

    // programa novo arquiva o anterior
    var second =
        JsonPath.<String>read(
            json(
                api.asJson(
                    pro,
                    post("/v1/coaching-links/" + linkId + "/programs"),
                    "{\"name\": \"Força\", \"scheduleMode\": \"sequence\", \"weeklyTarget\": 3}")),
            "$.id");
    api.as(pro, get("/v1/coaching-links/" + linkId + "/programs/active"))
        .andExpect(jsonPath("$.id").value(second));
  }

  @Test
  void editarTreinoComSessaoCriaVersaoNovaEASessaoFicaNaAntiga() throws Exception {
    var programId =
        JsonPath.<String>read(
            json(
                api.asJson(
                    pro,
                    post("/v1/coaching-links/" + linkId + "/programs"),
                    "{\"name\": \"Hipertrofia\", \"scheduleMode\": \"sequence\","
                        + " \"weeklyTarget\": 3}")),
            "$.id");
    var workoutId =
        JsonPath.<String>read(
            json(
                api.asJson(
                    pro, post("/v1/programs/" + programId + "/workouts"), "{\"name\": \"A\"}")),
            "$.id");
    api.asJson(
            pro,
            put("/v1/workouts/" + workoutId).header(HttpHeaders.IF_MATCH, "\"r1\""),
            content(bench, "55"))
        .andExpect(jsonPath("$.versionNumber").value(1));

    // o aluno treinou nessa versão
    var originalVersion =
        (UUID) single("select current_version_id from workout where id = ?::uuid", workoutId);
    var sessionId = UUID.randomUUID();
    exec(
        "insert into workout_session(id, client_id, coaching_link_id, program_id, workout_id,"
            + " workout_version_id, status, started_at, finished_at, performed_by,"
            + " performed_by_user, client_updated_at)"
            + " select ?, p.client_id, p.coaching_link_id, p.id, ?::uuid, ?, 'completed',"
            + " now() - interval '1 hour', now(), 'client', c.user_id, now()"
            + " from program p join client c on c.id = p.client_id where p.id = ?::uuid",
        sessionId,
        workoutId,
        originalVersion,
        programId);

    api.asJson(
            pro,
            put("/v1/workouts/" + workoutId).header(HttpHeaders.IF_MATCH, "\"r2\""),
            content(bench, "60"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.versionNumber").value(2))
        .andExpect(jsonPath("$.content.blocks[0].exercises[0].sets[0].loadKg").value(60));

    var newVersion =
        (UUID) single("select current_version_id from workout where id = ?::uuid", workoutId);
    assertThat(newVersion).isNotEqualTo(originalVersion);
    assertThat(single("select workout_version_id from workout_session where id = ?", sessionId))
        .isEqualTo(originalVersion);
    // o planejado original continua lá para comparar: 55 kg
    assertThat(
            single(
                    "select s.load_kg from prescribed_set s"
                        + " join prescribed_exercise e on e.id = s.prescribed_exercise_id"
                        + " join workout_block b on b.id = e.block_id"
                        + " where b.workout_version_id = ?",
                    originalVersion)
                .toString())
        .startsWith("55");
  }

  @Test
  void outroPersonalNaoMexeNoProgramaDoAluno() throws Exception {
    var programId =
        JsonPath.<String>read(
            json(
                api.asJson(
                    pro,
                    post("/v1/coaching-links/" + linkId + "/programs"),
                    "{\"name\": \"Hipertrofia\", \"scheduleMode\": \"fixed_days\"}")),
            "$.id");
    var other = api.professional("Carlos Lima", "Studio Fit");

    expectProblem(api.as(other, get("/v1/programs/" + programId)), 404, "resource-not-found");
    expectProblem(
        api.as(other, get("/v1/coaching-links/" + linkId + "/programs/active")),
        404,
        "resource-not-found");
    expectProblem(
        api.asJson(other, post("/v1/programs/" + programId + "/workouts"), "{\"name\": \"X\"}"),
        404,
        "resource-not-found");

    // aluno inativo fica congelado
    api.as(pro, post("/v1/coaching-links/" + linkId + "/inactivate"))
        .andExpect(status().isNoContent());
    expectProblem(
        api.asJson(pro, post("/v1/programs/" + programId + "/workouts"), "{\"name\": \"X\"}"),
        409,
        "link-not-trainable");
  }
}
