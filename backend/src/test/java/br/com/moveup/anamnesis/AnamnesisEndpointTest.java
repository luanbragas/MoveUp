package br.com.moveup.anamnesis;

import static br.com.moveup.db.DbFixtures.exec;
import static br.com.moveup.db.DbFixtures.single;
import static br.com.moveup.support.ApiActors.expectProblem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.moveup.support.ApiActors;
import br.com.moveup.support.TestJwt;
import br.com.moveup.support.WorkerHarness;
import com.jayway.jsonpath.JsonPath;
import java.time.Clock;
import java.time.Duration;
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
 * Fase 5, critério de pronto: um dump do banco não mostra respostas da anamnese em texto, e a
 * restrição aparece ao montar treino para o aluno certo (e só para ele).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestJwt.Keys.class)
class AnamnesisEndpointTest {

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    TestJwt.registerProperties(registry);
  }

  private static final String MEDICATION = "Losartana 50 mg para pressão";

  @Autowired MockMvc mvc;
  ApiActors api;
  String pro;
  String student;
  String linkId;
  UUID clientId;

  @BeforeEach
  void setUp() throws Exception {
    exec("update outbox_event set processed_at = now() where processed_at is null");
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
  }

  private static String answers(boolean parqYes, String medication) {
    return """
        {"answers": {
          "goal": "hypertrophy", "activity_level": "beginner",
          "weekly_days": 3, "session_minutes": 60,
          "parq_heart": false, "parq_chest_exercise": false, "parq_chest_rest": false,
          "parq_dizziness": false, "parq_bone_joint": %s, "parq_medication": false,
          "parq_other": false,
          "pain_regions": ["knee_left"],
          "medications": "%s"
        }}
        """
        .formatted(parqYes, medication);
  }

  @Test
  void dumpDoBancoNaoMostraRespostasEmTexto() throws Exception {
    api.as(student, get("/v1/anamnesis/template"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.questions[?(@.section == 'parq')]", hasSize(7)));

    api.asJson(student, put("/v1/me/anamnesis"), answers(true, MEDICATION))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.versionNumber").value(1))
        .andExpect(jsonPath("$.parqPositive").value(true))
        .andExpect(jsonPath("$.clearance").value("pending"))
        .andExpect(jsonPath("$.answers.medications").value(MEDICATION));

    // o que um dump veria: só o texto cifrado (e as colunas de ação abertas)
    var raw =
        single("select answers::text from anamnesis where client_id = ?", clientId).toString();
    assertThat(raw).startsWith("\"v1.").doesNotContain("Losartana").doesNotContain("knee");
    assertThat(single("select parq_positive from anamnesis where client_id = ?", clientId))
        .isEqualTo(true);
    assertThat(single("select goal from anamnesis where client_id = ?", clientId))
        .isEqualTo("hypertrophy");
    assertThat(count("select count(*) from client_key where client_id = ?", clientId)).isEqualTo(1);

    // o aluno lê de volta em claro
    api.as(student, get("/v1/me/anamnesis"))
        .andExpect(jsonPath("$.anamnesis.answers.pain_regions[0]").value("knee_left"));
  }

  @Test
  void versaoTravaDepoisDaRevisaoEOPersonalLiberaComData() throws Exception {
    api.asJson(student, put("/v1/me/anamnesis"), answers(true, "nenhum"))
        .andExpect(status().isOk());
    // antes da revisão, reenviar substitui a mesma versão
    api.asJson(student, put("/v1/me/anamnesis"), answers(true, "Losartana"))
        .andExpect(jsonPath("$.versionNumber").value(1));

    var other = api.professional("Caio Lima", "Studio Caio");
    expectProblem(
        api.as(other, get("/v1/clients/" + linkId + "/anamnesis")), 404, "resource-not-found");

    api.as(pro, get("/v1/clients/" + linkId + "/anamnesis"))
        .andExpect(jsonPath("$.latest.answers.medications").value("Losartana"))
        .andExpect(jsonPath("$.versions", hasSize(1)));
    assertThat(
            count(
                "select count(*) from audit_log where client_id = ? and action = 'view_anamnesis'",
                clientId))
        .isEqualTo(1);

    expectProblem(
        api.asJson(
            pro,
            post("/v1/clients/" + linkId + "/anamnesis/review"),
            answers(true, "Losartana").replace("}}", "}, \"clearance\": \"cleared\"}")),
        422,
        "clearance-date-required");
    api.asJson(
            pro,
            post("/v1/clients/" + linkId + "/anamnesis/review"),
            answers(true, "Losartana 50 mg")
                .replace("}}", "}, \"clearance\": \"cleared\", \"clearanceDate\": \"2026-10-01\"}"))
        .andExpect(jsonPath("$.reviewed").value(true))
        .andExpect(jsonPath("$.clearance").value("cleared"));

    // revisada é imutável: no banco também
    assertThat(
            catchSql(() -> exec("update anamnesis set goal = 'x' where client_id = ?", clientId)))
        .contains("imutável");

    // o aluno atualiza: vira a versão 2
    api.asJson(student, put("/v1/me/anamnesis"), answers(false, "nenhum"))
        .andExpect(jsonPath("$.versionNumber").value(2))
        .andExpect(jsonPath("$.clearance").value("not_required"));
    api.as(pro, get("/v1/clients/" + linkId + "/anamnesis/versions/1"))
        .andExpect(jsonPath("$.answers.medications").value("Losartana 50 mg"));
  }

  @Test
  void liberacaoPendenteAbreAlertaEARevisaoResolve() throws Exception {
    var worker = new WorkerHarness(Clock.offset(Clock.systemUTC(), Duration.ofHours(-1)));
    api.asJson(student, put("/v1/me/anamnesis"), answers(true, "nenhum"))
        .andExpect(status().isOk());
    worker.outbox().processAvailable(10);
    assertThat(openClearanceAlerts()).isEqualTo(1);

    api.asJson(
            pro,
            post("/v1/clients/" + linkId + "/anamnesis/review"),
            answers(true, "nenhum")
                .replace("}}", "}, \"clearance\": \"cleared\", \"clearanceDate\": \"2026-10-02\"}"))
        .andExpect(status().isOk());
    worker.outbox().processAvailable(10);
    assertThat(openClearanceAlerts()).isZero();
  }

  @Test
  void restricaoCifradaSoParaOAlunoCerto() throws Exception {
    var created =
        api.asJson(
                pro,
                post("/v1/clients/" + linkId + "/restrictions"),
                "{\"kind\": \"injury\", \"bodyRegion\": \"knee_left\", \"description\":"
                    + " \"Lesão no menisco medial\", \"severity\": 2}")
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String id = JsonPath.read(created, "$.id");

    api.as(pro, get("/v1/clients/" + linkId + "/restrictions"))
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].bodyRegion").value("knee_left"))
        .andExpect(jsonPath("$[0].description").value("Lesão no menisco medial"));
    assertThat(
            single("select description from health_restriction where id = ?::uuid", id).toString())
        .startsWith("v1.")
        .doesNotContain("menisco");

    // outro personal: não vê nem mexe
    var other = api.professional("Caio Lima", "Studio Caio");
    expectProblem(
        api.as(other, get("/v1/clients/" + linkId + "/restrictions")), 404, "resource-not-found");
    expectProblem(
        api.as(other, delete("/v1/clients/" + linkId + "/restrictions/" + id)),
        404,
        "resource-not-found");

    // resolvida sai da lista de ativas; excluir some de vez
    api.asJson(
            pro,
            put("/v1/clients/" + linkId + "/restrictions/" + id),
            "{\"kind\": \"injury\", \"bodyRegion\": \"knee_left\", \"description\":"
                + " \"Lesão no menisco medial\", \"resolvedOn\": \"2026-10-03\"}")
        .andExpect(jsonPath("$.resolvedOn").value("2026-10-03"));
    api.as(pro, get("/v1/clients/" + linkId + "/restrictions"))
        .andExpect(jsonPath("$", hasSize(0)));
    api.as(pro, get("/v1/clients/" + linkId + "/restrictions").param("includeResolved", "true"))
        .andExpect(jsonPath("$", hasSize(1)));
    api.as(pro, delete("/v1/clients/" + linkId + "/restrictions/" + id))
        .andExpect(status().isNoContent());
    api.as(pro, get("/v1/clients/" + linkId + "/restrictions").param("includeResolved", "true"))
        .andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  void comentarioEDorDoTreinoTambemVaoCifrados() throws Exception {
    var id = UUID.randomUUID();
    var body =
        """
        {"sessions": [{
          "id": "%s", "linkId": "%s", "status": "completed",
          "startedAt": "2026-10-05T12:00:00Z", "finishedAt": "2026-10-05T13:00:00Z",
          "clientUpdatedAt": "2026-10-05T13:00:00Z", "exercises": [],
          "feedback": {"effort": 6, "comment": "Joelho estalou no agachamento",
            "pains": [{"id": "%s", "bodyRegion": "knee_left", "intensity": 5,
                       "description": "Dor aguda ao descer"}]}
        }]}
        """
            .formatted(id, linkId, UUID.randomUUID());
    api.asJson(student, post("/v1/sync"), body).andExpect(status().isOk());

    assertThat(single("select comment from session_feedback where session_id = ?", id).toString())
        .startsWith("v1.")
        .doesNotContain("Joelho");
    assertThat(single("select description from pain_report where session_id = ?", id).toString())
        .startsWith("v1.")
        .doesNotContain("Dor");
  }

  private long openClearanceAlerts() throws Exception {
    return count(
        "select count(*) from alert where client_id = ? and type = 'clearance_pending'"
            + " and status <> 'resolved'",
        clientId);
  }

  private static long count(String sql, Object... params) throws Exception {
    return ((Number) single(sql, params)).longValue();
  }

  private static String catchSql(SqlCall call) {
    try {
      call.run();
      return "";
    } catch (java.sql.SQLException e) {
      return e.getMessage();
    }
  }

  @FunctionalInterface
  interface SqlCall {
    void run() throws java.sql.SQLException;
  }
}
