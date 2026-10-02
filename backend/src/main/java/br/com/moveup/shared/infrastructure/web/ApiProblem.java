package br.com.moveup.shared.infrastructure.web;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Forma do {@code ProblemDetail} que a API devolve, só para documentar o contrato no OpenAPI (o
 * {@code ProblemDetail} do Spring guarda {@code code}, {@code traceId} e {@code errors} num mapa
 * genérico, o que geraria um schema impreciso para o app).
 */
@Schema(name = "Problem", description = "Erro no formato RFC 9457 (application/problem+json)")
public record ApiProblem(
    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = Problems.TYPE_BASE + "resource-not-found")
        String type,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "Recurso não encontrado")
        String title,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "404") int status,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "Recurso não encontrado.")
        String detail,
    @Schema(example = "/v1/me") String instance,
    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            description = "Código estável (kebab-case) que o app usa para escolher a mensagem",
            example = "resource-not-found")
        String code,
    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "4bf92f3577b34da6a3ce929d0e0e4736")
        String traceId,
    @Schema(description = "Só em erros de validação (400)") List<FieldProblem> errors) {

  @Schema(name = "FieldProblem")
  public record FieldProblem(
      @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "reps.max") String field,
      @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "min") String code,
      @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String message) {}
}
