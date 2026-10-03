package br.com.moveup.training.infrastructure.web;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import br.com.moveup.shared.domain.Forbidden;
import br.com.moveup.shared.infrastructure.security.CurrentAppUser;
import br.com.moveup.shared.infrastructure.web.ApiProblem;
import br.com.moveup.training.application.port.in.ManagePrograms;
import br.com.moveup.training.application.port.in.ManagePrograms.ProgramInput;
import br.com.moveup.training.application.port.in.ManagePrograms.ProgramView;
import br.com.moveup.training.application.port.in.ManagePrograms.SlotInput;
import br.com.moveup.training.application.port.in.ManageWorkouts.WorkoutView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Programa do aluno e agenda dos treinos (SCREEN-FLOWS 2.3). */
@RestController
@Tag(name = "programs", description = "Programas e agenda")
@ApiResponse(
    responseCode = "404",
    description = "`resource-not-found` (vínculo ou programa de outro profissional)",
    content =
        @Content(
            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
            schema = @Schema(implementation = ApiProblem.class)))
class ProgramsController {

  private final ManagePrograms programs;
  private final CurrentAppUser currentAppUser;

  ProgramsController(ManagePrograms programs, CurrentAppUser currentAppUser) {
    this.programs = programs;
    this.currentAppUser = currentAppUser;
  }

  @Schema(name = "ProgramInput")
  record ProgramRequest(
      @Schema(requiredMode = REQUIRED) @NotBlank @Size(max = 80) String name,
      @Size(max = 200) String goal,
      LocalDate startsOn,
      LocalDate endsOn,
      @Schema(
              requiredMode = REQUIRED,
              allowableValues = {"fixed_days", "sequence"})
          @NotBlank
          String scheduleMode,
      @Schema(description = "Treinos por semana (obrigatório na sequência, 1 a 14)")
          Integer weeklyTarget) {

    ProgramInput toInput() {
      return new ProgramInput(name, goal, startsOn, endsOn, scheduleMode, weeklyTarget);
    }
  }

  @Schema(name = "ProgramSlot")
  record SlotDto(
      @Schema(requiredMode = REQUIRED) @NotNull UUID workoutId,
      @ArraySchema(
              arraySchema = @Schema(description = "0 = domingo; só no modo dias fixos"),
              schema = @Schema(minimum = "0", maximum = "6"))
          Set<Integer> weekdays) {}

  @Schema(name = "ProgramUpdate")
  record ProgramUpdateRequest(
      @Schema(requiredMode = REQUIRED) @NotBlank @Size(max = 80) String name,
      @Size(max = 200) String goal,
      LocalDate startsOn,
      LocalDate endsOn,
      @Schema(
              requiredMode = REQUIRED,
              allowableValues = {"fixed_days", "sequence"})
          @NotBlank
          String scheduleMode,
      Integer weeklyTarget,
      @ArraySchema(
              arraySchema =
                  @Schema(
                      requiredMode = REQUIRED,
                      description = "Todos os treinos do programa, na ordem (A, B, C…)"))
          @NotNull
          @Size(max = 14)
          List<@Valid SlotDto> schedule) {}

  @Schema(name = "NewProgramWorkout")
  record NewWorkoutRequest(
      @Schema(nullable = true, description = "Modelo a copiar; ausente = treino vazio")
          UUID templateId,
      @Schema(nullable = true, description = "Padrão: o nome do modelo") @Size(max = 60)
          String name) {}

  @Schema(name = "ProgramWorkout")
  record ProgramWorkoutResponse(
      @Schema(requiredMode = REQUIRED) UUID id,
      @Schema(requiredMode = REQUIRED) String name,
      @Schema(requiredMode = REQUIRED, description = "Ordem na agenda (A = 1)") int position,
      @ArraySchema(arraySchema = @Schema(requiredMode = REQUIRED)) Set<Integer> weekdays,
      Integer estimatedMinutes,
      @Schema(requiredMode = REQUIRED) int exercises) {}

  @Schema(name = "Program")
  record ProgramResponse(
      @Schema(requiredMode = REQUIRED) UUID id,
      @Schema(requiredMode = REQUIRED) UUID linkId,
      @Schema(requiredMode = REQUIRED) String name,
      String goal,
      LocalDate startsOn,
      LocalDate endsOn,
      @Schema(
              requiredMode = REQUIRED,
              allowableValues = {"fixed_days", "sequence"})
          String scheduleMode,
      Integer weeklyTarget,
      @Schema(requiredMode = REQUIRED, description = "Mande em If-Match como \"r<revision>\"")
          int revision,
      @Schema(requiredMode = REQUIRED) List<ProgramWorkoutResponse> workouts) {

    static ProgramResponse from(ProgramView v) {
      return new ProgramResponse(
          v.id(),
          v.linkId(),
          v.name(),
          v.goal(),
          v.startsOn(),
          v.endsOn(),
          v.scheduleMode(),
          v.weeklyTarget(),
          v.revision(),
          v.workouts().stream()
              .map(
                  w ->
                      new ProgramWorkoutResponse(
                          w.id(),
                          w.name(),
                          w.position(),
                          w.weekdays(),
                          w.estimatedMinutes(),
                          w.exercises()))
              .toList());
    }
  }

  @PostMapping(
      path = "/v1/coaching-links/{linkId}/programs",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      operationId = "createProgram",
      summary = "Cria o programa do aluno",
      description = "Vira o programa ativo; o anterior do vínculo é arquivado.")
  @ApiResponse(responseCode = "201", description = "Programa criado (cabeçalho ETag)")
  @ApiResponse(
      responseCode = "409",
      description = "`link-not-trainable` (aluno inativo ou encerrado)",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
              schema = @Schema(implementation = ApiProblem.class)))
  @ApiResponse(
      responseCode = "422",
      description = "`program-name-invalid`, `period-invalid`, `weekly-target-invalid`…",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
              schema = @Schema(implementation = ApiProblem.class)))
  ResponseEntity<ProgramResponse> create(
      @PathVariable UUID linkId, @Valid @RequestBody ProgramRequest request) {
    return withEtag(
        ResponseEntity.status(HttpStatus.CREATED),
        programs.create(currentUser(), linkId, request.toInput()));
  }

  @GetMapping(
      path = "/v1/coaching-links/{linkId}/programs/active",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(operationId = "getActiveProgram", summary = "Programa ativo do aluno")
  @ApiResponse(responseCode = "200", description = "Programa ativo (cabeçalho ETag)")
  @ApiResponse(responseCode = "204", description = "Aluno sem programa ativo")
  ResponseEntity<ProgramResponse> active(@PathVariable UUID linkId) {
    return programs
        .active(currentUser(), linkId)
        .map(view -> withEtag(ResponseEntity.ok(), view))
        .orElseGet(() -> ResponseEntity.noContent().build());
  }

  @GetMapping(path = "/v1/programs/{programId}", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(operationId = "getProgram", summary = "Programa com os treinos e a agenda")
  @ApiResponse(responseCode = "200", description = "Programa (cabeçalho ETag)")
  ResponseEntity<ProgramResponse> get(@PathVariable UUID programId) {
    return withEtag(ResponseEntity.ok(), programs.get(currentUser(), programId));
  }

  @PutMapping(
      path = "/v1/programs/{programId}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      operationId = "updateProgram",
      summary = "Salva nome, período e agenda",
      description = "A agenda lista todos os treinos do programa, na ordem desejada.")
  @ApiResponse(responseCode = "200", description = "Programa salvo (cabeçalho ETag)")
  @ApiResponse(
      responseCode = "412",
      description = "`version-mismatch`",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
              schema = @Schema(implementation = ApiProblem.class)))
  @ApiResponse(
      responseCode = "422",
      description = "`schedule-workouts-invalid`, `weekday-invalid`…",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
              schema = @Schema(implementation = ApiProblem.class)))
  ResponseEntity<ProgramResponse> update(
      @PathVariable UUID programId,
      @Parameter(description = "\"r<revision>\" da última leitura", required = true)
          @RequestHeader(name = HttpHeaders.IF_MATCH, required = false)
          String ifMatch,
      @Valid @RequestBody ProgramUpdateRequest request) {
    var view =
        programs.update(
            currentUser(),
            programId,
            Revisions.fromIfMatch(ifMatch),
            new ProgramInput(
                request.name(),
                request.goal(),
                request.startsOn(),
                request.endsOn(),
                request.scheduleMode(),
                request.weeklyTarget()),
            request.schedule().stream()
                .map(s -> new SlotInput(s.workoutId(), s.weekdays()))
                .toList());
    return withEtag(ResponseEntity.ok(), view);
  }

  @PostMapping(
      path = "/v1/programs/{programId}/workouts",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      operationId = "addProgramWorkout",
      summary = "Adiciona um treino ao programa",
      description = "Cópia de um modelo (guarda a origem) ou treino vazio para abrir no editor.")
  @ApiResponse(
      responseCode = "201",
      description = "Treino criado (edite com PUT /v1/workouts/{id})")
  WorkoutsController.WorkoutResponse addWorkout(
      @PathVariable UUID programId, @Valid @RequestBody NewWorkoutRequest request) {
    WorkoutView view =
        programs.addWorkout(currentUser(), programId, request.templateId(), request.name());
    return WorkoutsController.WorkoutResponse.from(view);
  }

  @DeleteMapping("/v1/programs/{programId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(operationId = "archiveProgram", summary = "Arquiva o programa")
  @ApiResponse(responseCode = "204", description = "Arquivado")
  void archive(
      @PathVariable UUID programId,
      @Parameter(description = "\"r<revision>\" da última leitura", required = true)
          @RequestHeader(name = HttpHeaders.IF_MATCH, required = false)
          String ifMatch) {
    programs.archive(currentUser(), programId, Revisions.fromIfMatch(ifMatch));
  }

  private static ResponseEntity<ProgramResponse> withEtag(
      ResponseEntity.BodyBuilder builder, ProgramView view) {
    return builder.eTag(Revisions.etag(view.revision())).body(ProgramResponse.from(view));
  }

  private UUID currentUser() {
    return currentAppUser.id().orElseThrow(Forbidden::new);
  }
}
