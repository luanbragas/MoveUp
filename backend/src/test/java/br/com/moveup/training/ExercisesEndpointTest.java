package br.com.moveup.training;

import static br.com.moveup.support.ApiActors.expectProblem;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** Biblioteca de exercícios pela API (Fase 2, F2-1). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestJwt.Keys.class)
class ExercisesEndpointTest {

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

  @Test
  void buscaSemAcentoNaBaseComFiltroDeMusculo() throws Exception {
    var pro = api.professional("Ana Souza", "Studio Ana");

    // parte do nome, sem acento e em maiúsculas; o começo do nome vem primeiro
    api.as(pro, get("/v1/exercises").param("q", "SUPINO"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name", startsWith("Supino")))
        .andExpect(jsonPath("$[*].name", hasItem("Supino reto com barra")))
        .andExpect(jsonPath("$[0].custom").value(false));
    api.as(pro, get("/v1/exercises").param("q", "extensao lombar"))
        .andExpect(jsonPath("$[*].name", hasItem("Hiperextensão lombar")));
    // nome com erro de digitação ainda acha pelo parecido
    api.as(pro, get("/v1/exercises").param("q", "agachamneto"))
        .andExpect(jsonPath("$[*].name", hasItem("Agachamento livre com barra")));
    api.as(pro, get("/v1/exercises").param("muscle", "hamstrings").param("limit", "100"))
        .andExpect(jsonPath("$[*].name", hasItem("Mesa flexora")))
        .andExpect(jsonPath("$[*].name", hasItem("Stiff com barra")))
        .andExpect(jsonPath("$[*].name", not(hasItem("Supino reto com barra"))));
    expectProblem(
        api.as(pro, get("/v1/exercises").param("muscle", "nariz")), 422, "muscle-invalid");
  }

  @Test
  void exercicioProprioSoNaOrganizacaoDoPersonal() throws Exception {
    var ana = api.professional("Ana Souza", "Studio Ana");
    var carlos = api.professional("Carlos Lima", "Studio Fit");

    var created =
        api.asJson(
                ana,
                post("/v1/exercises"),
                """
                {"name": "  Remada  cavalinho ", "modality": "strength", "trackingType": "reps_load",
                 "primaryMuscle": "lats", "secondaryMuscles": ["biceps", "traps"],
                 "equipment": "barra T", "mediaUrl": "https://youtu.be/exemplo"}
                """)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Remada cavalinho"))
            .andExpect(jsonPath("$.custom").value(true))
            .andReturn()
            .getResponse()
            .getContentAsString();
    var id = JsonPath.<String>read(created, "$.id");

    api.as(ana, get("/v1/exercises").param("q", "cavalinho"))
        .andExpect(jsonPath("$[*].id", hasItem(id)));
    api.as(carlos, get("/v1/exercises").param("q", "cavalinho"))
        .andExpect(jsonPath("$[*].id", not(hasItem(id))));

    // mesmo nome (sem acento/caixa) na organização ou na base: conflito
    expectProblem(
        api.asJson(
            ana,
            post("/v1/exercises"),
            "{\"name\": \"REMADA CAVALINHO\", \"modality\": \"strength\", \"trackingType\":"
                + " \"reps_load\"}"),
        409,
        "exercise-name-taken");
    expectProblem(
        api.asJson(
            ana,
            post("/v1/exercises"),
            "{\"name\": \"Prancha\", \"modality\": \"complementary\", \"trackingType\": \"time\"}"),
        409,
        "exercise-name-taken");

    // outro personal não arquiva; o dono arquiva e some da busca
    expectProblem(api.as(carlos, delete("/v1/exercises/" + id)), 404, "resource-not-found");
    api.as(ana, delete("/v1/exercises/" + id)).andExpect(status().isNoContent());
    api.as(ana, get("/v1/exercises").param("q", "cavalinho"))
        .andExpect(jsonPath("$[*].id", not(hasItem(id))));
  }

  @Test
  void baseNaoMudaEDadoRuimTemCodigo() throws Exception {
    var pro = api.professional("Ana Souza", "Studio Ana");
    var base =
        JsonPath.<String>read(
            api.as(pro, get("/v1/exercises").param("q", "Prancha"))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$[0].id");

    expectProblem(api.as(pro, delete("/v1/exercises/" + base)), 409, "base-exercise-read-only");
    expectProblem(
        api.asJson(
            pro,
            post("/v1/exercises"),
            "{\"name\": \"Xis\", \"modality\": \"strength\", \"trackingType\": \"reps_load\","
                + " \"mediaUrl\": \"http://inseguro.test/v\"}"),
        422,
        "media-url-invalid");
    expectProblem(
        api.asJson(
            pro,
            post("/v1/exercises"),
            "{\"name\": \"Xis\", \"modality\": \"strength\", \"trackingType\": \"reps_load\","
                + " \"primaryMuscle\": \"chest\", \"secondaryMuscles\": [\"chest\"]}"),
        422,
        "muscle-invalid");
  }

  @Test
  void alunoNaoUsaABiblioteca() throws Exception {
    var student = api.client("Bia Lima");

    expectProblem(api.as(student, get("/v1/exercises")), 403, "forbidden");
    api.as(api.professional("Ana", "Studio"), get("/v1/exercises"))
        .andExpect(jsonPath("$[*].custom", everyItem(org.hamcrest.Matchers.is(false))));
  }
}
