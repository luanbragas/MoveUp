package br.com.moveup.alerts;

import static br.com.moveup.db.DbFixtures.exec;
import static br.com.moveup.db.DbFixtures.single;
import static br.com.moveup.support.ApiActors.expectProblem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.moveup.alerts.application.port.out.PushGateway;
import br.com.moveup.shared.application.outbox.OutboxEvent;
import br.com.moveup.shared.application.outbox.OutboxHandler;
import br.com.moveup.support.ApiActors;
import br.com.moveup.support.TestJwt;
import br.com.moveup.support.WorkerHarness;
import com.jayway.jsonpath.JsonPath;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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

/**
 * Fase 4, critério de pronto: derrubar o worker no meio do processamento e religar não perde nem
 * duplica recorde, alerta ou push. O app envia pela API (app_api); o worker roda como app_worker.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestJwt.Keys.class)
class WorkerPipelineTest {

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    TestJwt.registerProperties(registry);
  }

  /** O relógio do worker uma hora atrás: a nova tentativa já está vencida no banco. */
  private static final Clock BEHIND = Clock.offset(Clock.systemUTC(), Duration.ofHours(-1));

  @Autowired MockMvc mvc;
  ApiActors api;
  WorkerHarness worker;
  String pro;
  String student;
  String linkId;
  UUID clientId;
  String bench;

  @BeforeEach
  void setUp() throws Exception {
    // eventos de outros testes não entram na conta deste
    exec("update outbox_event set processed_at = now() where processed_at is null");
    worker = new WorkerHarness(BEHIND);
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
    clientId = (UUID) single("select client_id from coaching_link where id = ?::uuid", linkId);
    bench =
        JsonPath.read(
            api.as(pro, get("/v1/exercises").param("q", "Supino reto com barra"))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$[0].id");
  }

  private String session(UUID id, String updatedAt, int load, int effort, boolean pain) {
    var exerciseId = UUID.nameUUIDFromBytes((id + "e").getBytes());
    var painJson =
        pain
            ? "{\"id\": \"%s\", \"bodyRegion\": \"knee_left\", \"intensity\": 6}"
                .formatted(UUID.nameUUIDFromBytes((id + "p").getBytes()))
            : "";
    return """
        {"sessions": [{
          "id": "%s", "linkId": "%s", "status": "completed",
          "startedAt": "2026-10-05T12:00:00Z", "finishedAt": "2026-10-05T13:00:00Z",
          "durationSeconds": 3600, "completionRatio": 1, "clientUpdatedAt": "%s",
          "exercises": [{"id": "%s", "exerciseId": "%s", "position": 1, "status": "done",
            "sets": [
              {"id": "%s", "setNumber": 1, "reps": 10, "loadKg": %d, "completed": true,
               "completedAt": "%s"},
              {"id": "%s", "setNumber": 2, "reps": 8, "loadKg": %d, "completed": true,
               "completedAt": "%s"}]}],
          "feedback": {"effort": %d, "comment": "Joelho incomodou", "pains": [%s]}
        }]}
        """
        .formatted(
            id,
            linkId,
            updatedAt,
            exerciseId,
            bench,
            UUID.nameUUIDFromBytes((id + "s1").getBytes()),
            load,
            updatedAt,
            UUID.nameUUIDFromBytes((id + "s2").getBytes()),
            load,
            updatedAt,
            effort,
            painJson);
  }

  private long count(String sql, Object... params) throws Exception {
    return ((Number) single(sql, params)).longValue();
  }

  private long alerts(String type) throws Exception {
    return count(
        "select count(*) from alert where client_id = ? and type = ? and status <> 'resolved'",
        clientId,
        type);
  }

  /** Handler que derruba a transação na primeira vez, depois de recordes e alertas gravarem. */
  static final class CrashOnce implements OutboxHandler {
    int calls;

    @Override
    public Set<String> types() {
      return Set.of("session.finished", "session.edited");
    }

    @Override
    public void handle(OutboxEvent event) {
      if (calls++ == 0) {
        throw new IllegalStateException("worker caiu");
      }
    }
  }

  static final class FakeExpo implements PushGateway {
    final List<Message> sent = new ArrayList<>();

    @Override
    public List<Result> send(List<Message> messages) {
      sent.addAll(messages);
      return messages.stream().map(m -> new Result(true, false, false, null)).toList();
    }
  }

  @Test
  void workerCaiNoMeioEReligaSemPerderNemDuplicar() throws Exception {
    var sessionId = UUID.randomUUID();
    api.asJson(student, post("/v1/sync"), session(sessionId, "2026-10-05T13:00:00Z", 60, 9, true))
        .andExpect(status().isOk());
    var crash = new CrashOnce();
    worker.handlers.add(crash);
    var outbox = worker.outbox();

    // 1ª tentativa: recordes e alertas gravam, e o worker "cai" antes do commit
    assertThat(outbox.processOne()).isTrue();
    assertThat(count("select count(*) from personal_record where client_id = ?", clientId))
        .isZero();
    assertThat(alerts("pain_reported")).isZero();
    assertThat(
            single(
                "select attempts from outbox_event where aggregate_id = ? and type ="
                    + " 'session.finished'",
                sessionId))
        .isEqualTo(1);

    // religou: processa de novo, uma vez só
    assertThat(outbox.processOne()).isTrue();
    assertThat(outbox.processOne()).isFalse();

    // recordes: maior carga (60) e reps nessa carga (10), vigentes
    assertThat(
            count(
                "select count(*) from personal_record where client_id = ? and is_current",
                clientId))
        .isEqualTo(2);
    assertThat(
            single(
                "select value from personal_record where client_id = ? and is_current"
                    + " and record_type = 'max_load'",
                clientId))
        .hasToString("60.000");
    // alertas: dor (urgente), esforço 9 e comentário; um de cada
    assertThat(alerts("pain_reported")).isEqualTo(1);
    assertThat(alerts("high_effort")).isEqualTo(1);
    assertThat(alerts("new_feedback")).isEqualTo(1);
    // push só da dor (padrão), com texto neutro
    assertThat(
            count(
                "select count(*) from push_message where user_id = (select professional_id"
                    + " from coaching_link where id = ?::uuid)",
                linkId))
        .isEqualTo(1);
    assertThat(
            single(
                "select body from push_message where user_id = (select professional_id from"
                    + " coaching_link where id = ?::uuid)",
                linkId))
        .asString()
        .doesNotContain("Bia")
        .doesNotContain("Joelho")
        .doesNotContain("dor");

    // o mesmo evento entregue de novo (ex.: commit perdido): nada muda
    exec(
        "update outbox_event set processed_at = null, next_attempt_at = now() - interval '1"
            + " minute' where aggregate_id = ?",
        sessionId);
    assertThat(outbox.processOne()).isTrue();
    assertThat(
            count(
                "select count(*) from personal_record where client_id = ? and is_current",
                clientId))
        .isEqualTo(2);
    assertThat(alerts("pain_reported")).isEqualTo(1);
    assertThat(
            count(
                "select count(*) from push_message where dedupe_key like 'alert:%' and"
                    + " user_id = (select professional_id from coaching_link where id = ?::uuid)",
                linkId))
        .isEqualTo(1);
  }

  @Test
  void pushNuncaDuplicaMesmoComOWorkerCaindoNoEnvio() throws Exception {
    api.asJson(
            pro,
            put("/v1/push-devices"),
            "{\"token\": \"ExponentPushToken[abcdefgh12345]\", \"platform\": \"android\"}")
        .andExpect(status().isNoContent());
    var first = UUID.randomUUID();
    api.asJson(student, post("/v1/sync"), session(first, "2026-10-05T13:00:00Z", 60, 5, true))
        .andExpect(status().isOk());
    worker.outbox().processAvailable(10);

    // o worker pegou o push (sending, confirmado) e caiu antes de chamar o Expo
    worker.tx.execute(status -> worker.pushQueue.claim(10, Instant.now()));
    var expo = new FakeExpo();
    // religou: o preso vira failed e não é reenviado
    assertThat(worker.pushes(expo, Duration.ZERO).sendBatch()).isZero();
    assertThat(expo.sent).isEmpty();

    // caminho normal: outra dor, um push, entregue uma vez
    var second = UUID.randomUUID();
    api.asJson(student, post("/v1/sync"), session(second, "2026-10-06T13:00:00Z", 62, 5, true))
        .andExpect(status().isOk());
    worker.outbox().processAvailable(10);
    var sender = worker.pushes(expo, Duration.ofMinutes(10));
    assertThat(sender.sendBatch()).isEqualTo(1);
    assertThat(sender.sendBatch()).isZero();
    assertThat(expo.sent).hasSize(1);
    assertThat(expo.sent.getFirst().body()).isEqualTo("Um aluno precisa da sua atenção.");
  }

  @Test
  void inatividadeDiariaSemDuplicarEResolvidaQuandoOAlunoTreina() throws Exception {
    exec(
        "update coaching_link set started_at = ? where id = ?::uuid",
        Instant.now().minus(Duration.ofDays(10)).atOffset(ZoneOffset.UTC),
        linkId);
    worker.tx.executeWithoutResult(s -> worker.daily().run());
    worker.tx.executeWithoutResult(s -> worker.daily().run());
    assertThat(alerts("inactive")).isEqualTo(1);

    var sessionId = UUID.randomUUID();
    api.asJson(student, post("/v1/sync"), session(sessionId, "2026-10-05T13:00:00Z", 50, 5, false))
        .andExpect(status().isOk());
    worker.outbox().processAvailable(10);
    assertThat(alerts("inactive")).isZero();
  }

  @Test
  void centralDeAtencaoListaResolveAdiaEConfigura() throws Exception {
    api.asJson(
            student,
            post("/v1/sync"),
            session(UUID.randomUUID(), "2026-10-05T13:00:00Z", 60, 9, true))
        .andExpect(status().isOk());
    worker.outbox().processAvailable(10);

    var page =
        api.as(pro, get("/v1/alerts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items", hasSize(3)))
            .andExpect(jsonPath("$.openCount").value(3))
            .andExpect(jsonPath("$.items[0].clientName").value("Bia Lima"))
            .andExpect(jsonPath("$.items[0].linkId").value(linkId))
            .andReturn()
            .getResponse()
            .getContentAsString();
    // a lista não carrega o que o aluno escreveu nem a região da dor
    assertThat(page).doesNotContain("Joelho").doesNotContain("knee").doesNotContain("null");

    String first = JsonPath.read(page, "$.items[0].id");
    String second = JsonPath.read(page, "$.items[1].id");
    api.as(pro, post("/v1/alerts/" + first + "/resolve")).andExpect(status().isNoContent());
    api.asJson(pro, post("/v1/alerts/" + second + "/snooze"), "{\"days\": 3}")
        .andExpect(status().isNoContent());
    api.as(pro, get("/v1/alerts")).andExpect(jsonPath("$.openCount").value(1));
    api.as(pro, get("/v1/alerts").param("status", "snoozed"))
        .andExpect(jsonPath("$.items", hasSize(1)));
    expectProblem(
        api.asJson(pro, post("/v1/alerts/" + first + "/snooze"), "{\"days\": 3}"),
        422,
        "alert-resolved");
    // o aluno não enxerga a central
    expectProblem(api.as(student, get("/v1/alerts")), 403, "forbidden");

    api.asJson(
            pro,
            put("/v1/alert-settings"),
            "{\"settings\": [{\"type\": \"inactive\", \"enabled\": true, \"push\": false,"
                + " \"threshold\": 5}]}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.type == 'inactive')].threshold").value(5));
    expectProblem(
        api.asJson(
            pro,
            put("/v1/alert-settings"),
            "{\"settings\": [{\"type\": \"inactive\", \"enabled\": true, \"push\": false,"
                + " \"threshold\": 1}]}"),
        422,
        "threshold-invalid");
  }
}
