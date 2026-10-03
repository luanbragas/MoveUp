package br.com.moveup.training.infrastructure.web;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import br.com.moveup.shared.domain.Forbidden;
import br.com.moveup.shared.infrastructure.security.CurrentAppUser;
import br.com.moveup.shared.infrastructure.web.ApiProblem;
import br.com.moveup.training.application.port.in.PlannedSync;
import br.com.moveup.training.application.port.in.PlannedSync.ProgramSnapshot;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Recebimento do sync do aluno: o planejado que mudou (ARQUITETURA 4). */
@RestController
@Tag(name = "sync", description = "Sincronização offline do app do aluno")
class SyncController {

  private final PlannedSync sync;
  private final CurrentAppUser currentAppUser;

  SyncController(PlannedSync sync, CurrentAppUser currentAppUser) {
    this.sync = sync;
    this.currentAppUser = currentAppUser;
  }

  @Schema(name = "SyncWorkout")
  record WorkoutDto(
      @Schema(requiredMode = REQUIRED) UUID id,
      @Schema(requiredMode = REQUIRED) String name,
      @Schema(requiredMode = REQUIRED, description = "Ordem na agenda (A = 1)") int position,
      @ArraySchema(arraySchema = @Schema(requiredMode = REQUIRED, description = "0 = domingo"))
          Set<Integer> weekdays,
      @Schema(requiredMode = REQUIRED, description = "Vai na sessão (o planejado daquele dia)")
          UUID versionId,
      @Schema(requiredMode = REQUIRED) int versionNumber,
      @Schema(requiredMode = REQUIRED) WorkoutsController.ContentResponse content) {}

  @Schema(name = "SyncProgram")
  record ProgramDto(
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
      @Schema(requiredMode = REQUIRED, description = "Tombstone: apague o programa e os treinos")
          boolean deleted,
      @Schema(
              requiredMode = REQUIRED,
              description = "Todos os treinos ativos, na ordem (substituem os do app)")
          List<WorkoutDto> workouts) {

    static ProgramDto from(ProgramSnapshot p) {
      return new ProgramDto(
          p.id(),
          p.linkId(),
          p.name(),
          p.goal(),
          p.startsOn(),
          p.endsOn(),
          p.scheduleMode(),
          p.weeklyTarget(),
          p.deleted(),
          p.workouts().stream()
              .map(
                  w ->
                      new WorkoutDto(
                          w.id(),
                          w.name(),
                          w.position(),
                          w.weekdays(),
                          w.versionId(),
                          w.versionNumber(),
                          WorkoutsController.ContentResponse.from(w.content())))
              .toList());
    }
  }

  @Schema(name = "SyncChanges")
  record ChangesResponse(
      @Schema(requiredMode = REQUIRED, description = "Mande em ?since= na próxima vez")
          Instant cursor,
      @Schema(requiredMode = REQUIRED) List<ProgramDto> programs,
      @Schema(requiredMode = REQUIRED, description = "Exercícios usados nos treinos enviados")
          List<ExercisesController.ExerciseResponse> exercises) {}

  @GetMapping(path = "/v1/sync", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      operationId = "getSyncChanges",
      summary = "O que mudou no planejado do aluno",
      description =
          "Programas (com treinos e agenda) alterados desde o cursor, com janela de 2 minutos;"
              + " aplique por upsert. Sem cursor, tudo que vale hoje.")
  @ApiResponse(responseCode = "200", description = "Mudanças e o cursor novo")
  @ApiResponse(
      responseCode = "403",
      description = "`forbidden` (conta não é de aluno)",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
              schema = @Schema(implementation = ApiProblem.class)))
  ChangesResponse changes(
      @Parameter(description = "Cursor da última sincronização (ISO-8601)")
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant since) {
    var page = sync.changesSince(currentUser(), since);
    return new ChangesResponse(
        page.cursor(),
        page.programs().stream().map(ProgramDto::from).toList(),
        page.exercises().stream().map(ExercisesController.ExerciseResponse::from).toList());
  }

  private UUID currentUser() {
    return currentAppUser.id().orElseThrow(Forbidden::new);
  }
}
