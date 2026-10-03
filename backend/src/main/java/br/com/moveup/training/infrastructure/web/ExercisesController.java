package br.com.moveup.training.infrastructure.web;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import br.com.moveup.shared.domain.Forbidden;
import br.com.moveup.shared.infrastructure.security.CurrentAppUser;
import br.com.moveup.shared.infrastructure.web.ApiProblem;
import br.com.moveup.training.application.port.in.ManageExercises;
import br.com.moveup.training.application.port.in.ManageExercises.ExerciseView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Biblioteca de exercícios do personal (SCREEN-FLOWS 2.1). */
@RestController
@Tag(name = "exercises", description = "Biblioteca de exercícios")
@ApiResponse(
    responseCode = "403",
    description = "Conta não é de profissional (`forbidden`)",
    content =
        @Content(
            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
            schema = @Schema(implementation = ApiProblem.class)))
class ExercisesController {

  private final ManageExercises exercises;
  private final CurrentAppUser currentAppUser;

  ExercisesController(ManageExercises exercises, CurrentAppUser currentAppUser) {
    this.exercises = exercises;
    this.currentAppUser = currentAppUser;
  }

  @Schema(name = "Exercise")
  record ExerciseResponse(
      @Schema(requiredMode = REQUIRED) UUID id,
      @Schema(requiredMode = REQUIRED) String name,
      @Schema(
              requiredMode = REQUIRED,
              allowableValues = {"strength", "cardio", "conditioning", "complementary"})
          String modality,
      @Schema(
              requiredMode = REQUIRED,
              description = "O que o aluno registra em cada série",
              allowableValues = {"reps_load", "reps_only", "time", "distance_time"})
          String trackingType,
      @Schema(nullable = true, description = "Código do mapa muscular") String primaryMuscle,
      @ArraySchema(arraySchema = @Schema(requiredMode = REQUIRED)) List<String> secondaryMuscles,
      @Schema(nullable = true) String equipment,
      @Schema(requiredMode = REQUIRED) boolean unilateral,
      @Schema(nullable = true, description = "Como executar") String instructions,
      @Schema(nullable = true, description = "Vídeo (link https)") String mediaUrl,
      @Schema(requiredMode = REQUIRED, description = "Exercício próprio (pode arquivar)")
          boolean custom) {

    static ExerciseResponse from(ExerciseView v) {
      return new ExerciseResponse(
          v.id(),
          v.name(),
          v.modality(),
          v.trackingType(),
          v.primaryMuscle(),
          v.secondaryMuscles(),
          v.equipment(),
          v.unilateral(),
          v.instructions(),
          v.mediaUrl(),
          v.custom());
    }
  }

  @Schema(name = "NewExercise")
  record NewExerciseRequest(
      @Schema(requiredMode = REQUIRED) @NotBlank @Size(max = 120) String name,
      @Schema(
              requiredMode = REQUIRED,
              allowableValues = {"strength", "cardio", "conditioning", "complementary"})
          @NotBlank
          String modality,
      @Schema(
              requiredMode = REQUIRED,
              allowableValues = {"reps_load", "reps_only", "time", "distance_time"})
          @NotBlank
          String trackingType,
      @Schema(nullable = true) String primaryMuscle,
      @Size(max = 6) List<String> secondaryMuscles,
      @Size(max = 60) String equipment,
      @Schema(description = "Padrão: false") Boolean unilateral,
      @Size(max = 2000) String instructions,
      @Size(max = 500) String mediaUrl) {}

  @GetMapping(path = "/v1/exercises", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      operationId = "searchExercises",
      summary = "Busca na biblioteca",
      description =
          "Base do MoveUp + exercícios próprios. Busca por parte do nome ou nome parecido, sem"
              + " acento; filtro opcional por músculo (principal ou secundário).")
  @ApiResponse(responseCode = "200", description = "Até `limit` exercícios (máx. 100)")
  List<ExerciseResponse> search(
      @RequestParam(required = false) String q,
      @Schema(
              allowableValues = {
                "chest",
                "delts",
                "traps",
                "abs",
                "lats",
                "biceps",
                "triceps",
                "forearms",
                "quads",
                "adductors",
                "abductors",
                "calves",
                "lowerback",
                "glutes",
                "hamstrings"
              })
          @RequestParam(required = false)
          String muscle,
      @RequestParam(defaultValue = "50") int limit) {
    return exercises.search(currentUser(), q, muscle, limit).stream()
        .map(ExerciseResponse::from)
        .toList();
  }

  @PostMapping(
      path = "/v1/exercises",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(operationId = "createExercise", summary = "Cria um exercício próprio")
  @ApiResponse(responseCode = "201", description = "Exercício criado")
  @ApiResponse(
      responseCode = "409",
      description = "`exercise-name-taken`",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
              schema = @Schema(implementation = ApiProblem.class)))
  @ApiResponse(
      responseCode = "422",
      description =
          "`exercise-name-invalid`, `modality-invalid`, `tracking-type-invalid`,"
              + " `muscle-invalid`, `instructions-invalid`, `media-url-invalid`",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
              schema = @Schema(implementation = ApiProblem.class)))
  ExerciseResponse create(@Valid @RequestBody NewExerciseRequest request) {
    return ExerciseResponse.from(
        exercises.create(
            currentUser(),
            new ManageExercises.NewExercise(
                request.name(),
                request.modality(),
                request.trackingType(),
                request.primaryMuscle(),
                request.secondaryMuscles(),
                request.equipment(),
                Boolean.TRUE.equals(request.unilateral()),
                request.instructions(),
                request.mediaUrl())));
  }

  @DeleteMapping("/v1/exercises/{exerciseId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      operationId = "archiveExercise",
      summary = "Arquiva um exercício próprio",
      description = "Some da busca; treinos que já usam continuam mostrando.")
  @ApiResponse(responseCode = "204", description = "Arquivado")
  @ApiResponse(
      responseCode = "409",
      description = "`base-exercise-read-only`",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
              schema = @Schema(implementation = ApiProblem.class)))
  void archive(@PathVariable UUID exerciseId) {
    exercises.archive(currentUser(), exerciseId);
  }

  private UUID currentUser() {
    return currentAppUser.id().orElseThrow(Forbidden::new);
  }
}
