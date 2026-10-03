package br.com.moveup.anamnesis.infrastructure.web;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import br.com.moveup.anamnesis.application.port.in.ManageAnamnesis;
import br.com.moveup.anamnesis.application.port.in.ManageAnamnesis.AnamnesisView;
import br.com.moveup.anamnesis.application.port.in.ManageRestrictions;
import br.com.moveup.anamnesis.application.port.in.ManageRestrictions.RestrictionInput;
import br.com.moveup.anamnesis.application.port.in.ManageRestrictions.RestrictionView;
import br.com.moveup.anamnesis.domain.model.Question;
import br.com.moveup.shared.domain.Forbidden;
import br.com.moveup.shared.infrastructure.security.CurrentAppUser;
import br.com.moveup.shared.infrastructure.web.ApiProblem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Anamnese e restrições (SCREEN-FLOWS 1.2 e 2.2). Dado de saúde: nunca em log. */
@RestController
@Tag(name = "anamnesis", description = "Anamnese do aluno e restrições de saúde")
@ApiResponse(
    responseCode = "404",
    description = "`resource-not-found` (vínculo de outro profissional, versão inexistente)",
    content =
        @Content(
            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
            schema = @Schema(implementation = ApiProblem.class)))
class AnamnesisController {

  private final ManageAnamnesis anamnesis;
  private final ManageRestrictions restrictions;
  private final CurrentAppUser currentAppUser;

  AnamnesisController(
      ManageAnamnesis anamnesis, ManageRestrictions restrictions, CurrentAppUser currentAppUser) {
    this.anamnesis = anamnesis;
    this.restrictions = restrictions;
    this.currentAppUser = currentAppUser;
  }

  @Schema(name = "AnamnesisOption")
  record OptionDto(
      @Schema(requiredMode = REQUIRED) String value,
      @Schema(requiredMode = REQUIRED) String label) {}

  @Schema(name = "AnamnesisQuestion")
  record QuestionDto(
      @Schema(requiredMode = REQUIRED) String code,
      @Schema(
              requiredMode = REQUIRED,
              allowableValues = {"goal", "routine", "parq", "health"})
          String section,
      @Schema(requiredMode = REQUIRED) String label,
      @Schema(
              requiredMode = REQUIRED,
              allowableValues = {"single", "multi", "yes_no", "integer", "text"})
          String type,
      @Schema(requiredMode = REQUIRED) boolean required,
      @Schema(requiredMode = REQUIRED) List<OptionDto> options,
      Integer min,
      Integer max) {

    static QuestionDto of(Question q) {
      return new QuestionDto(
          q.code(),
          q.section(),
          q.label(),
          q.type().code(),
          q.required(),
          q.options().stream().map(o -> new OptionDto(o.value(), o.label())).toList(),
          q.min(),
          q.max());
    }
  }

  @Schema(name = "AnamnesisTemplate")
  record TemplateResponse(
      @Schema(requiredMode = REQUIRED) int version,
      @Schema(requiredMode = REQUIRED) List<QuestionDto> questions) {}

  @Schema(name = "Anamnesis")
  record AnamnesisResponse(
      @Schema(requiredMode = REQUIRED) int versionNumber,
      @Schema(
              requiredMode = REQUIRED,
              description = "Código da pergunta → resposta (texto, sim/não, número ou lista)")
          Map<String, Object> answers,
      @Schema(requiredMode = REQUIRED) boolean parqPositive,
      @Schema(
              requiredMode = REQUIRED,
              allowableValues = {"not_required", "pending", "cleared"})
          String clearance,
      LocalDate clearanceDate,
      @Schema(requiredMode = REQUIRED) boolean reviewed,
      Instant reviewedAt,
      @Schema(requiredMode = REQUIRED) Instant createdAt) {

    static AnamnesisResponse of(AnamnesisView v) {
      return new AnamnesisResponse(
          v.versionNumber(),
          v.answers(),
          v.parqPositive(),
          v.clearance(),
          v.clearanceDate(),
          v.reviewed(),
          v.reviewedAt(),
          v.createdAt());
    }
  }

  @Schema(name = "MyAnamnesis")
  record MyAnamnesisResponse(
      @Schema(description = "Ausente antes do primeiro envio") AnamnesisResponse anamnesis) {}

  @Schema(name = "AnamnesisSubmit")
  record SubmitRequest(
      @Schema(requiredMode = REQUIRED) @NotNull @Size(max = 100) Map<String, Object> answers) {}

  @Schema(name = "AnamnesisVersion")
  record VersionResponse(
      @Schema(requiredMode = REQUIRED) int versionNumber,
      @Schema(requiredMode = REQUIRED) Instant createdAt,
      Instant reviewedAt,
      @Schema(
              requiredMode = REQUIRED,
              allowableValues = {"not_required", "pending", "cleared"})
          String clearance,
      @Schema(requiredMode = REQUIRED) boolean parqPositive) {}

  @Schema(name = "ClientAnamnesis")
  record ClientAnamnesisResponse(
      @Schema(description = "Ausente se o aluno ainda não enviou") AnamnesisResponse latest,
      @Schema(requiredMode = REQUIRED) List<VersionResponse> versions) {}

  @Schema(name = "AnamnesisReview")
  record ReviewRequest(
      @Schema(requiredMode = REQUIRED, description = "Respostas com o complemento do personal")
          @NotNull
          @Size(max = 100)
          Map<String, Object> answers,
      @Schema(
              requiredMode = REQUIRED,
              allowableValues = {"not_required", "pending", "cleared"})
          @NotBlank
          String clearance,
      @Schema(description = "Obrigatória quando clearance = cleared") LocalDate clearanceDate) {}

  @Schema(name = "HealthRestriction")
  record RestrictionResponse(
      @Schema(requiredMode = REQUIRED) UUID id,
      @Schema(
              requiredMode = REQUIRED,
              allowableValues = {"injury", "surgery", "pain", "condition"})
          String kind,
      @Schema(description = "Mesma lista de regiões do relato de dor") String bodyRegion,
      @Schema(requiredMode = REQUIRED) String description,
      @Schema(description = "1 leve, 2 moderada, 3 grave") Integer severity,
      @Schema(description = "Ausente = ativa") LocalDate resolvedOn,
      @Schema(requiredMode = REQUIRED) boolean fromAnamnesis) {

    static RestrictionResponse of(RestrictionView v) {
      return new RestrictionResponse(
          v.id(),
          v.kind(),
          v.bodyRegion(),
          v.description(),
          v.severity(),
          v.resolvedOn(),
          v.fromAnamnesis());
    }
  }

  @Schema(name = "HealthRestrictionInput")
  record RestrictionRequest(
      @Schema(
              requiredMode = REQUIRED,
              allowableValues = {"injury", "surgery", "pain", "condition"})
          @NotBlank
          String kind,
      String bodyRegion,
      @Schema(requiredMode = REQUIRED) @NotBlank @Size(max = 500) String description,
      Integer severity,
      LocalDate resolvedOn) {

    RestrictionInput toInput() {
      return new RestrictionInput(kind, bodyRegion, description, severity, resolvedOn);
    }
  }

  @GetMapping(path = "/v1/anamnesis/template", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(operationId = "getAnamnesisTemplate", summary = "Perguntas da anamnese (modelo atual)")
  @ApiResponse(responseCode = "200", description = "Modelo do sistema com PAR-Q")
  TemplateResponse template() {
    user();
    var t = anamnesis.template();
    return new TemplateResponse(t.version(), t.questions().stream().map(QuestionDto::of).toList());
  }

  @GetMapping(path = "/v1/me/anamnesis", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(operationId = "getMyAnamnesis", summary = "A anamnese mais recente do aluno")
  @ApiResponse(responseCode = "200", description = "Sem anamnese, o campo vem ausente")
  MyAnamnesisResponse mine() {
    return new MyAnamnesisResponse(anamnesis.mine(user()).map(AnamnesisResponse::of).orElse(null));
  }

  @PutMapping(
      path = "/v1/me/anamnesis",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      operationId = "submitMyAnamnesis",
      summary = "Envia a anamnese",
      description =
          "Antes da revisão do personal substitui a versão atual; depois, cria uma versão nova.")
  @ApiResponse(responseCode = "200", description = "Versão gravada")
  @ApiResponse(
      responseCode = "422",
      description =
          "`answer-invalid`, `answer-required`, `answer-unknown`, `anamnesis-without-link`",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
              schema = @Schema(implementation = ApiProblem.class)))
  AnamnesisResponse submit(@Valid @RequestBody SubmitRequest request) {
    return AnamnesisResponse.of(anamnesis.submit(user(), request.answers()));
  }

  @GetMapping(path = "/v1/clients/{linkId}/anamnesis", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      operationId = "getClientAnamnesis",
      summary = "Anamnese do aluno (mais recente e versões)",
      description = "Abrir fica registrado na trilha de auditoria (view_anamnesis).")
  @ApiResponse(responseCode = "200", description = "Mais recente e lista de versões")
  ClientAnamnesisResponse ofClient(@PathVariable UUID linkId) {
    var result = anamnesis.ofLink(user(), linkId);
    return new ClientAnamnesisResponse(
        result.latest().map(AnamnesisResponse::of).orElse(null),
        result.versions().stream()
            .map(
                v ->
                    new VersionResponse(
                        v.versionNumber(), v.createdAt(), v.reviewedAt(), v.clearance(), v.parq()))
            .toList());
  }

  @GetMapping(
      path = "/v1/clients/{linkId}/anamnesis/versions/{versionNumber}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(operationId = "getClientAnamnesisVersion", summary = "Uma versão da anamnese")
  @ApiResponse(responseCode = "200", description = "A versão")
  AnamnesisResponse version(@PathVariable UUID linkId, @PathVariable int versionNumber) {
    return AnamnesisResponse.of(anamnesis.version(user(), linkId, versionNumber));
  }

  @PostMapping(
      path = "/v1/clients/{linkId}/anamnesis/review",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      operationId = "reviewClientAnamnesis",
      summary = "Revisa a anamnese (complemento e liberação médica)",
      description = "Revisada fica imutável; revisar de novo cria uma versão nova.")
  @ApiResponse(responseCode = "200", description = "Versão revisada")
  @ApiResponse(
      responseCode = "422",
      description = "`answer-invalid`, `clearance-invalid`, `clearance-date-required`",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
              schema = @Schema(implementation = ApiProblem.class)))
  AnamnesisResponse review(@PathVariable UUID linkId, @Valid @RequestBody ReviewRequest request) {
    return AnamnesisResponse.of(
        anamnesis.review(
            user(), linkId, request.answers(), request.clearance(), request.clearanceDate()));
  }

  @GetMapping(
      path = "/v1/clients/{linkId}/restrictions",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(operationId = "listRestrictions", summary = "Restrições do aluno (ativas primeiro)")
  @ApiResponse(responseCode = "200", description = "Restrições")
  List<RestrictionResponse> restrictions(
      @PathVariable UUID linkId, @RequestParam(defaultValue = "false") boolean includeResolved) {
    return restrictions.list(user(), linkId, includeResolved).stream()
        .map(RestrictionResponse::of)
        .toList();
  }

  @PostMapping(
      path = "/v1/clients/{linkId}/restrictions",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(operationId = "createRestriction", summary = "Nova restrição")
  @ApiResponse(responseCode = "201", description = "Criada")
  @ApiResponse(
      responseCode = "422",
      description =
          "`restriction-kind-invalid`, `restriction-region-invalid`,"
              + " `restriction-description-invalid`, `restriction-severity-invalid`",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
              schema = @Schema(implementation = ApiProblem.class)))
  RestrictionResponse create(
      @PathVariable UUID linkId, @Valid @RequestBody RestrictionRequest request) {
    return RestrictionResponse.of(restrictions.create(user(), linkId, request.toInput()));
  }

  @PutMapping(
      path = "/v1/clients/{linkId}/restrictions/{restrictionId}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(operationId = "updateRestriction", summary = "Altera ou resolve uma restrição")
  @ApiResponse(responseCode = "200", description = "Alterada")
  RestrictionResponse update(
      @PathVariable UUID linkId,
      @PathVariable UUID restrictionId,
      @Valid @RequestBody RestrictionRequest request) {
    return RestrictionResponse.of(
        restrictions.update(user(), linkId, restrictionId, request.toInput()));
  }

  @DeleteMapping("/v1/clients/{linkId}/restrictions/{restrictionId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(operationId = "deleteRestriction", summary = "Exclui uma restrição lançada errado")
  @ApiResponse(responseCode = "204", description = "Excluída")
  void delete(@PathVariable UUID linkId, @PathVariable UUID restrictionId) {
    restrictions.delete(user(), linkId, restrictionId);
  }

  private UUID user() {
    return currentAppUser.id().orElseThrow(Forbidden::new);
  }
}
