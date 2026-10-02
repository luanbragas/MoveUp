package br.com.moveup.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import br.com.moveup.db.PostgresTestDatabase;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * O contrato commitado ({@code backend/openapi/openapi.yaml}) tem que ser igual ao gerado pelo
 * código (BACKEND-PATTERN, seção 9). Mudou a API de propósito: regrave com {@code ./mvnw test
 * -Dtest=OpenApiContractTest -Dopenapi.update=true} e rode {@code pnpm api:generate} no mesmo PR.
 */
@SpringBootTest(
    properties = {"springdoc.api-docs.enabled=true", "moveup.auth.firebase.project-id=contract"})
@AutoConfigureMockMvc
class OpenApiContractTest {

  private static final Path CONTRACT = Path.of("openapi", "openapi.yaml");

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry registry) {
    PostgresTestDatabase.registerSpringProperties(registry);
  }

  @Autowired MockMvc mvc;

  @Test
  void contratoCommitadoEstaAtualizado() throws Exception {
    var generated =
        normalize(
            mvc.perform(get("/v3/api-docs.yaml"))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8));

    if (Boolean.parseBoolean(System.getProperty("openapi.update"))) {
      Files.createDirectories(CONTRACT.getParent());
      Files.writeString(CONTRACT, generated, StandardCharsets.UTF_8);
    }

    assertThat(generated).contains("/v1/me", "bearerAuth", "Problem");
    assertThat(CONTRACT).as("contrato não encontrado; gere com -Dopenapi.update=true").exists();
    assertThat(normalize(Files.readString(CONTRACT, StandardCharsets.UTF_8)))
        .as(
            "openapi.yaml desatualizado: se a mudança de contrato é intencional, rode ./mvnw test"
                + " -Dtest=OpenApiContractTest -Dopenapi.update=true e pnpm api:generate")
        .isEqualTo(generated);
  }

  private static String normalize(String yaml) {
    var unix = yaml.replace("\r\n", "\n");
    return unix.endsWith("\n") ? unix : unix + "\n";
  }
}
