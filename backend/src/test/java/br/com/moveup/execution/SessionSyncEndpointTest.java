package br.com.moveup.execution;

import static br.com.moveup.db.DbFixtures.single;
import static br.com.moveup.support.ApiActors.expectProblem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** POST /v1/sync: treino registrado offline chega uma vez só (Fase 3, critério de pronto). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestJwt.Keys.class)
class SessionSyncEndpointTest {

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    TestJwt.registerProperties(registry);
  }

  @Autowired MockMvc mvc;
  ApiActors api;
  String pro;
  String student;
  String linkId;
  String bench;

  @BeforeEach
  void setUp() throws Exception {
    api = new ApiActors(mvc);
    pro = api.professional("Ana Souza", "Studio Ana");
    student = api.client("Bia Lima");
    var invite =
        api.asJson(pro, post("/v1/clients"), "{\"name\": \"Bia Lima\"}")
            .andReturn()
            .getResponse()
            .getContentAsString();
    linkId = JsonPath.read(invite, "$.linkId");
    api.as(student, post("/v1/invites/" + JsonPath.read(invite, "$.code") + "/accept"))
        .andExpect(status().isOk());
    bench =
        JsonPath.read(
            api.as(pro, get("/v1/exercises").param("q", "Supino reto com barra"))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$[0].id");
  }

  private String session(UUID id, String status, String updatedAt, int reps, String pain) {
    var exerciseId = UUID.nameUUIDFromBytes((id + "e").getBytes());
    var set1 = UUID.nameUUIDFromBytes((id + "s1").getBytes());
    var set2 = UUID.nameUUIDFromBytes((id + "s2").getBytes());
    var finished = status.equals("in_progress") ? "null" : "\"2026-10-05T13:00:00Z\"";
    return """
        {"sessions": [{
          "id": "%s", "linkId": "%s", "status": "%s",
          "startedAt": "2026-10-05T12:00:00Z", "finishedAt": %s, "durationSeconds": 3600,
          "completionRatio": 1, "clientUpdatedAt": "%s",
          "exercises": [{"id": "%s", "exerciseId": "%s", "position": 1, "status": "done",
            "sets": [
              {"id": "%s", "setNumber": 1, "reps": %d, "loadKg": 55, "completed": true},
              {"id": "%s", "setNumber": 2, "reps": 8, "loadKg": 55, "completed": true}]}],
          "feedback": {"effort": 7, "comment": "Pesado hoje", "pains": [%s]}
        }]}
        """
        .formatted(
            id, linkId, status, finished, updatedAt, exerciseId, bench, set1, reps, set2, pain);
  }

  private long count(String sql, UUID id) throws Exception {
    return ((Number) single(sql, id)).longValue();
  }

  @Test
  void treinoOfflineChegaUmaVezSoMesmoReenviado() throws Exception {
    var id = UUID.randomUUID();
    var pain =
        "{\"id\": \""
            + UUID.randomUUID()
            + "\", \"bodyRegion\": \"shoulder_right\", \"exerciseId\": \""
            + bench
            + "\", \"intensity\": 6}";
    var body = session(id, "completed", "2026-10-05T13:00:00Z", 10, pain);

    api.asJson(student, post("/v1/sync"), body)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.written", contains(id.toString())));
    // reenvio forçado do mesmo lote (ex.: resposta perdida): nada muda
    api.asJson(student, post("/v1/sync"), body)
        .andExpect(jsonPath("$.written", empty()))
        .andExpect(jsonPath("$.unchanged", contains(id.toString())));

    assertThat(count("select count(*) from workout_session where id = ?", id)).isEqualTo(1);
    assertThat(
            count(
                "select count(*) from performed_set s join performed_exercise e"
                    + " on e.id = s.performed_exercise_id where e.session_id = ?",
                id))
        .isEqualTo(2);
    assertThat(count("select count(*) from pain_report where session_id = ?", id)).isEqualTo(1);
    assertThat(single("select performed_by from workout_session where id = ?", id))
        .isEqualTo("client");
    assertThat(
            count(
                "select count(*) from outbox_event where aggregate_id = ? and type ="
                    + " 'session.finished'",
                id))
        .isEqualTo(1);
    // evento sem dado de saúde
    assertThat(
            single("select payload::text from outbox_event where aggregate_id = ?", id).toString())
        .doesNotContain("Pesado")
        .doesNotContain("shoulder");
  }

  @Test
  void edicaoDepoisDeFinalizarVenceEFicaMarcada() throws Exception {
    var id = UUID.randomUUID();
    api.asJson(student, post("/v1/sync"), session(id, "completed", "2026-10-05T13:00:00Z", 10, ""))
        .andExpect(status().isOk());

    // edição mais antiga que a gravada é ignorada
    api.asJson(student, post("/v1/sync"), session(id, "completed", "2026-10-05T12:59:00Z", 3, ""))
        .andExpect(jsonPath("$.unchanged", contains(id.toString())));
    // edição mais recente vence, substitui as séries e marca edited_after_finish_at
    api.asJson(student, post("/v1/sync"), session(id, "completed", "2026-10-05T14:00:00Z", 12, ""))
        .andExpect(jsonPath("$.written", contains(id.toString())));

    assertThat(
            single(
                "select s.reps from performed_set s join performed_exercise e"
                    + " on e.id = s.performed_exercise_id where e.session_id = ? and s.set_number = 1",
                id))
        .isEqualTo(12);
    assertThat(
            single(
                "select edited_after_finish_at is not null from workout_session where id = ?", id))
        .isEqualTo(true);
    assertThat(count("select count(*) from outbox_event where aggregate_id = ?", id)).isEqualTo(1);
  }

  @Test
  void sessaoEmAndamentoSoAvisaQuandoTermina() throws Exception {
    var id = UUID.randomUUID();
    api.asJson(
            student, post("/v1/sync"), session(id, "in_progress", "2026-10-05T12:30:00Z", 10, ""))
        .andExpect(status().isOk());
    assertThat(count("select count(*) from outbox_event where aggregate_id = ?", id)).isZero();

    api.asJson(student, post("/v1/sync"), session(id, "partial", "2026-10-05T13:00:00Z", 10, ""))
        .andExpect(status().isOk());
    assertThat(count("select count(*) from outbox_event where aggregate_id = ?", id)).isEqualTo(1);
    assertThat(
            single("select edited_after_finish_at is null from workout_session where id = ?", id))
        .isEqualTo(true);
  }

  @Test
  void presencialPeloPersonalEOutroAlunoNaoEnvia() throws Exception {
    var id = UUID.randomUUID();
    api.asJson(pro, post("/v1/sync"), session(id, "completed", "2026-10-05T13:00:00Z", 10, ""))
        .andExpect(jsonPath("$.written", contains(id.toString())));
    assertThat(single("select performed_by from workout_session where id = ?", id))
        .isEqualTo("professional");

    var other = api.client("Carla Dias");
    expectProblem(
        api.asJson(
            other,
            post("/v1/sync"),
            session(UUID.randomUUID(), "completed", "2026-10-05T13:00:00Z", 10, "")),
        404,
        "resource-not-found");
    expectProblem(
        api.asJson(
            student,
            post("/v1/sync"),
            session(
                UUID.randomUUID(),
                "completed",
                "2026-10-05T13:00:00Z",
                10,
                "{\"id\": \"" + UUID.randomUUID() + "\", \"bodyRegion\": \"nariz\"}")),
        422,
        "pain-invalid");
  }
}
