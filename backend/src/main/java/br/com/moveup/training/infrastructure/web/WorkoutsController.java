package br.com.moveup.training.infrastructure.web;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import br.com.moveup.shared.domain.Forbidden;
import br.com.moveup.shared.domain.VersionMismatch;
import br.com.moveup.shared.infrastructure.security.CurrentAppUser;
import br.com.moveup.shared.infrastructure.web.ApiProblem;
import br.com.moveup.training.application.port.in.ManageWorkouts;
import br.com.moveup.training.application.port.in.ManageWorkouts.BlockInput;
import br.com.moveup.training.application.port.in.ManageWorkouts.BlockView;
import br.com.moveup.training.application.port.in.ManageWorkouts.ContentInput;
import br.com.moveup.training.application.port.in.ManageWorkouts.ContentView;
import br.com.moveup.training.application.port.in.ManageWorkouts.ExerciseInput;
import br.com.moveup.training.application.port.in.ManageWorkouts.ExerciseView;
import br.com.moveup.training.application.port.in.ManageWorkouts.SetInput;
import br.com.moveup.training.application.port.in.ManageWorkouts.WorkoutSummary;
import br.com.moveup.training.application.port.in.ManageWorkouts.WorkoutView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
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

/**
 * Treinos do personal (SCREEN-FLOWS 2.3). O editor grava tudo de uma vez no "Salvar", com {@code
 * If-Match} = ETag do GET (ou {@code "r" + revision} do corpo).
 */
@RestController
@Tag(name = "workouts", description = "Treinos e modelos")
@ApiResponse(
    responseCode = "403",
    description = "Conta não é de profissional (`forbidden`)",
    content =
        @Content(
            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
            schema = @Schema(implementation = ApiProblem.class)))
class WorkoutsController {

  private static final Pattern ETAG = Pattern.compile("^(?:W/)?\"r(\\d{1,9})\"$");

  private final ManageWorkouts workouts;
  private final CurrentAppUser currentAppUser;

  WorkoutsController(ManageWorkouts workouts, CurrentAppUser currentAppUser) {
    this.workouts = workouts;
    this.currentAppUser = currentAppUser;
  }

  // ---------------------------------------------------------------- DTOs

  @Schema(name = "PrescribedSet")
  record SetDto(
      @Schema(
              description = "Padrão: normal",
              allowableValues = {"warmup", "normal", "drop", "rest_pause", "failure"})
          String type,
      @Schema(description = "Faixa: 8 a 12 = repsMin 8, repsMax 12") Integer repsMin,
      Integer repsMax,
      @Schema(description = "Carga em kg (até 3 casas)") BigDecimal loadKg,
      Integer durationSeconds,
      Integer distanceM,
      @Schema(description = "Esforço alvo 0 a 10 (use RPE ou RIR)") BigDecimal targetRpe,
      @Schema(description = "Repetições em reserva 0 a 10") Integer targetRir,
      Integer restSeconds) {

    SetInput toInput() {
      return new SetInput(
          type,
          repsMin,
          repsMax,
          loadKg,
          durationSeconds,
          distanceM,
          targetRpe,
          targetRir,
          restSeconds);
    }

    static SetDto from(SetInput s) {
      return new SetDto(
          s.type(),
          s.repsMin(),
          s.repsMax(),
          s.loadKg(),
          s.durationSeconds(),
          s.distanceM(),
          s.targetRpe(),
          s.targetRir(),
          s.restSeconds());
    }
  }

  @Schema(name = "PrescribedExerciseInput")
  record ExerciseDto(
      @Schema(requiredMode = REQUIRED) @NotNull UUID exerciseId,
      Integer restSeconds,
      @Size(max = 500) String notes,
      @Size(max = 20) List<@Valid SetDto> sets) {}

  @Schema(name = "WorkoutBlockInput")
  record BlockDto(
      @Size(max = 80) String name,
      @Schema(
              requiredMode = REQUIRED,
              allowableValues = {
                "sequential",
                "superset",
                "circuit",
                "hiit",
                "emom",
                "amrap",
                "intervals"
              })
          @NotBlank
          String method,
      @Schema(description = "Só `tabata` (HIIT 20 s/10 s × 8)", nullable = true) String preset,
      Integer rounds,
      Integer workSeconds,
      Integer restSeconds,
      Integer restBetweenRounds,
      @Schema(description = "EMOM e AMRAP") Integer durationSeconds,
      @Size(max = 500) String notes,
      @Schema(requiredMode = REQUIRED) @NotNull @Size(max = 12)
          List<@Valid ExerciseDto> exercises) {}

  @Schema(name = "WorkoutContentInput")
  record ContentDto(
      @Size(max = 120) String goal,
      Integer estimatedMinutes,
      @Size(max = 1000) String notes,
      @Schema(requiredMode = REQUIRED) @NotNull @Size(max = 20) List<@Valid BlockDto> blocks) {

    ContentInput toInput() {
      return new ContentInput(
          goal,
          estimatedMinutes,
          notes,
          blocks.stream()
              .map(
                  b ->
                      new BlockInput(
                          b.name(),
                          b.method(),
                          b.preset(),
                          b.rounds(),
                          b.workSeconds(),
                          b.restSeconds(),
                          b.restBetweenRounds(),
                          b.durationSeconds(),
                          b.notes(),
                          b.exercises().stream()
                              .map(
                                  e ->
                                      new ExerciseInput(
                                          e.exerciseId(),
                                          e.restSeconds(),
                                          e.notes(),
                                          e.sets() == null
                                              ? List.of()
                                              : e.sets().stream().map(SetDto::toInput).toList()))
                              .toList()))
              .toList());
    }
  }

  @Schema(name = "WorkoutWrite")
  record WorkoutWriteRequest(
      @Schema(requiredMode = REQUIRED) @NotBlank @Size(max = 60) String name,
      @Schema(requiredMode = REQUIRED) @NotNull @Valid ContentDto content) {}

  @Schema(name = "PrescribedExercise")
  record ExerciseResponse(
      @Schema(requiredMode = REQUIRED) UUID exerciseId,
      @Schema(nullable = true) String exerciseName,
      @Schema(
              nullable = true,
              allowableValues = {"reps_load", "reps_only", "time", "distance_time"})
          String trackingType,
      @Schema(nullable = true) String primaryMuscle,
      Integer restSeconds,
      String notes,
      @Schema(requiredMode = REQUIRED) List<SetDto> sets) {

    static ExerciseResponse from(ExerciseView e) {
      return new ExerciseResponse(
          e.exerciseId(),
          e.exerciseName(),
          e.trackingType(),
          e.primaryMuscle(),
          e.restSeconds(),
          e.notes(),
          e.sets().stream().map(SetDto::from).toList());
    }
  }

  @Schema(name = "WorkoutBlock")
  record BlockResponse(
      String name,
      @Schema(requiredMode = REQUIRED) String method,
      String preset,
      Integer rounds,
      Integer workSeconds,
      Integer restSeconds,
      Integer restBetweenRounds,
      Integer durationSeconds,
      String notes,
      @Schema(requiredMode = REQUIRED) List<ExerciseResponse> exercises) {

    static BlockResponse from(BlockView b) {
      return new BlockResponse(
          b.name(),
          b.method(),
          b.preset(),
          b.rounds(),
          b.workSeconds(),
          b.restSeconds(),
          b.restBetweenRounds(),
          b.durationSeconds(),
          b.notes(),
          b.exercises().stream().map(ExerciseResponse::from).toList());
    }
  }

  @Schema(name = "WorkoutContent")
  record ContentResponse(
      String goal,
      Integer estimatedMinutes,
      String notes,
      @Schema(requiredMode = REQUIRED) List<BlockResponse> blocks) {

    static ContentResponse from(ContentView c) {
      return new ContentResponse(
          c.goal(),
          c.estimatedMinutes(),
          c.notes(),
          c.blocks().stream().map(BlockResponse::from).toList());
    }
  }

  @Schema(name = "Workout")
  record WorkoutResponse(
      @Schema(requiredMode = REQUIRED) UUID id,
      @Schema(requiredMode = REQUIRED) String name,
      @Schema(requiredMode = REQUIRED, description = "Modelo (sem programa)") boolean template,
      @Schema(nullable = true) UUID programId,
      @Schema(nullable = true, description = "Modelo de origem da cópia") UUID sourceTemplateId,
      @Schema(requiredMode = REQUIRED, description = "Mande em If-Match como \"r<revision>\"")
          int revision,
      @Schema(requiredMode = REQUIRED, description = "Sobe quando o treino já tinha sessão")
          int versionNumber,
      @Schema(requiredMode = REQUIRED) ContentResponse content) {

    static WorkoutResponse from(WorkoutView v) {
      return new WorkoutResponse(
          v.id(),
          v.name(),
          v.template(),
          v.programId(),
          v.sourceTemplateId(),
          v.revision(),
          v.versionNumber(),
          ContentResponse.from(v.content()));
    }
  }

  @Schema(name = "WorkoutSummary")
  record SummaryResponse(
      @Schema(requiredMode = REQUIRED) UUID id,
      @Schema(requiredMode = REQUIRED) String name,
      @Schema(requiredMode = REQUIRED) int exercises,
      @Schema(requiredMode = REQUIRED) int blocks,
      Integer estimatedMinutes,
      @Schema(requiredMode = REQUIRED) Instant updatedAt) {

    static SummaryResponse from(WorkoutSummary s) {
      return new SummaryResponse(
          s.id(), s.name(), s.exercises(), s.blocks(), s.estimatedMinutes(), s.updatedAt());
    }
  }

  // ---------------------------------------------------------------- rotas

  @PostMapping(
      path = "/v1/workout-templates",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(operationId = "createWorkoutTemplate", summary = "Cria um modelo de treino")
  @ApiResponse(responseCode = "201", description = "Modelo criado (cabeçalho ETag com a revisão)")
  @ApiResponse(
      responseCode = "422",
      description = "Regra do treino (`block-empty`, `superset-needs-two`, `exercise-unknown`…)",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
              schema = @Schema(implementation = ApiProblem.class)))
  ResponseEntity<WorkoutResponse> createTemplate(@Valid @RequestBody WorkoutWriteRequest request) {
    var view = workouts.createTemplate(currentUser(), request.name(), request.content().toInput());
    return withEtag(ResponseEntity.status(HttpStatus.CREATED), view);
  }

  @GetMapping(path = "/v1/workout-templates", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      operationId = "listWorkoutTemplates",
      summary = "Modelos da organização",
      description = "Do editado mais recente ao mais antigo; sem os arquivados.")
  @ApiResponse(responseCode = "200", description = "Modelos")
  List<SummaryResponse> listTemplates() {
    return workouts.listTemplates(currentUser()).stream().map(SummaryResponse::from).toList();
  }

  @GetMapping(path = "/v1/workouts/{workoutId}", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(operationId = "getWorkout", summary = "Treino com o conteúdo da versão atual")
  @ApiResponse(responseCode = "200", description = "Treino (cabeçalho ETag com a revisão)")
  @ApiResponse(
      responseCode = "404",
      description = "`resource-not-found` (inexistente, arquivado ou de outra organização)",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
              schema = @Schema(implementation = ApiProblem.class)))
  ResponseEntity<WorkoutResponse> get(@PathVariable UUID workoutId) {
    return withEtag(ResponseEntity.ok(), workouts.get(currentUser(), workoutId));
  }

  @PutMapping(
      path = "/v1/workouts/{workoutId}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      operationId = "saveWorkout",
      summary = "Salva o treino inteiro",
      description =
          "Se a versão atual já foi usada numa sessão, cria versão nova (a sessão continua"
              + " comparando com o planejado dela); senão, substitui a atual.")
  @ApiResponse(responseCode = "200", description = "Treino salvo (cabeçalho ETag com a revisão)")
  @ApiResponse(
      responseCode = "412",
      description = "`version-mismatch` (salvo em outro aparelho)",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
              schema = @Schema(implementation = ApiProblem.class)))
  @ApiResponse(
      responseCode = "428",
      description = "`if-match-required`",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
              schema = @Schema(implementation = ApiProblem.class)))
  @ApiResponse(
      responseCode = "422",
      description = "Regra do treino",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
              schema = @Schema(implementation = ApiProblem.class)))
  ResponseEntity<WorkoutResponse> save(
      @PathVariable UUID workoutId,
      @Parameter(description = "\"r<revision>\" da última leitura", required = true)
          @RequestHeader(name = HttpHeaders.IF_MATCH, required = false)
          String ifMatch,
      @Valid @RequestBody WorkoutWriteRequest request) {
    var view =
        workouts.save(
            currentUser(),
            workoutId,
            revisionOf(ifMatch),
            request.name(),
            request.content().toInput());
    return withEtag(ResponseEntity.ok(), view);
  }

  @DeleteMapping("/v1/workouts/{workoutId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      operationId = "archiveWorkout",
      summary = "Arquiva o treino",
      description = "Some das listas e do app do aluno; sessões antigas continuam.")
  @ApiResponse(responseCode = "204", description = "Arquivado")
  @ApiResponse(
      responseCode = "412",
      description = "`version-mismatch`",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
              schema = @Schema(implementation = ApiProblem.class)))
  void archive(
      @PathVariable UUID workoutId,
      @Parameter(description = "\"r<revision>\" da última leitura", required = true)
          @RequestHeader(name = HttpHeaders.IF_MATCH, required = false)
          String ifMatch) {
    workouts.archive(currentUser(), workoutId, revisionOf(ifMatch));
  }

  // ---------------------------------------------------------------- apoio

  private static ResponseEntity<WorkoutResponse> withEtag(
      ResponseEntity.BodyBuilder builder, WorkoutView view) {
    return builder.eTag("\"r" + view.revision() + "\"").body(WorkoutResponse.from(view));
  }

  private static int revisionOf(String ifMatch) {
    if (ifMatch == null || ifMatch.isBlank()) {
      throw VersionMismatch.required();
    }
    var matcher = ETAG.matcher(ifMatch.strip());
    if (!matcher.matches()) {
      throw VersionMismatch.stale();
    }
    return Integer.parseInt(matcher.group(1));
  }

  private UUID currentUser() {
    return currentAppUser.id().orElseThrow(Forbidden::new);
  }
}
