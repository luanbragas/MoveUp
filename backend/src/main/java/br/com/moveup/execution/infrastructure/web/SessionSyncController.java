package br.com.moveup.execution.infrastructure.web;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import br.com.moveup.execution.application.port.in.SyncSessions;
import br.com.moveup.execution.domain.model.PerformedSession;
import br.com.moveup.execution.domain.model.PerformedSession.PerformedBy;
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
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Envio do sync: o treino registrado offline (ARQUITETURA 4). Dado de saúde: nunca em log. */
@RestController
@Tag(name = "sync", description = "Sincronização offline do app do aluno")
class SessionSyncController {

  private final SyncSessions sync;
  private final CurrentAppUser currentAppUser;

  SessionSyncController(SyncSessions sync, CurrentAppUser currentAppUser) {
    this.sync = sync;
    this.currentAppUser = currentAppUser;
  }

  @Schema(name = "PerformedSetInput")
  record SetDto(
      @Schema(requiredMode = REQUIRED, description = "Gerado no app (UUIDv7)") @NotNull UUID id,
      @Schema(requiredMode = REQUIRED) int setNumber,
      @Schema(allowableValues = {"warmup", "normal", "drop", "rest_pause", "failure"})
          String setType,
      @Schema(allowableValues = {"left", "right"}) String side,
      Integer reps,
      BigDecimal loadKg,
      Integer durationSeconds,
      Integer distanceM,
      BigDecimal rpe,
      Integer rir,
      @Schema(requiredMode = REQUIRED) boolean completed,
      Instant completedAt) {}

  @Schema(name = "PerformedExerciseInput")
  record ExerciseDto(
      @Schema(requiredMode = REQUIRED) @NotNull UUID id,
      @Schema(requiredMode = REQUIRED) @NotNull UUID exerciseId,
      @Schema(requiredMode = REQUIRED) int position,
      @Schema(
              requiredMode = REQUIRED,
              allowableValues = {"done", "skipped", "substituted"})
          @NotBlank
          String status,
      @Schema(description = "Exercício planejado que foi trocado") UUID substitutedFrom,
      @Size(max = 500) String notes,
      @Size(max = 30) List<@Valid SetDto> sets) {}

  @Schema(name = "PainInput")
  record PainDto(
      @Schema(requiredMode = REQUIRED) @NotNull UUID id,
      @Schema(
              requiredMode = REQUIRED,
              allowableValues = {
                "neck",
                "shoulder_left",
                "shoulder_right",
                "elbow_left",
                "elbow_right",
                "wrist_left",
                "wrist_right",
                "chest",
                "upper_back",
                "lower_back",
                "hip_left",
                "hip_right",
                "knee_left",
                "knee_right",
                "ankle_left",
                "ankle_right",
                "other"
              })
          @NotBlank
          String bodyRegion,
      UUID exerciseId,
      Integer intensity,
      @Size(max = 500) String description) {}

  @Schema(name = "SessionFeedbackInput")
  record FeedbackDto(
      @Schema(requiredMode = REQUIRED, description = "Esforço percebido 0–10") int effort,
      @Size(max = 1000) String comment,
      @Size(max = 10) List<@Valid PainDto> pains) {}

  @Schema(name = "PerformedSessionInput")
  record SessionDto(
      @Schema(requiredMode = REQUIRED) @NotNull UUID id,
      @Schema(requiredMode = REQUIRED) @NotNull UUID linkId,
      UUID programId,
      UUID workoutId,
      @Schema(description = "Versão do treino daquele dia (vem no GET /v1/sync)")
          UUID workoutVersionId,
      @Schema(
              requiredMode = REQUIRED,
              allowableValues = {"in_progress", "completed", "partial", "abandoned"})
          @NotBlank
          String status,
      @Schema(requiredMode = REQUIRED) @NotNull Instant startedAt,
      Instant finishedAt,
      Integer durationSeconds,
      @Schema(description = "0 a 1: base da adesão") BigDecimal completionRatio,
      @Schema(requiredMode = REQUIRED, description = "Hora da última edição no aparelho") @NotNull
          Instant clientUpdatedAt,
      @Size(max = 40) List<@Valid ExerciseDto> exercises,
      @Valid FeedbackDto feedback) {

    PerformedSession toDomain(PerformedBy by) {
      return new PerformedSession(
          id,
          linkId,
          programId,
          workoutId,
          workoutVersionId,
          PerformedSession.Status.fromCode(status),
          startedAt,
          finishedAt,
          durationSeconds,
          completionRatio,
          by,
          clientUpdatedAt,
          exercises == null
              ? List.of()
              : exercises.stream()
                  .map(
                      e ->
                          new PerformedSession.Exercise(
                              e.id(),
                              e.exerciseId(),
                              e.position(),
                              e.status(),
                              e.substitutedFrom(),
                              e.notes(),
                              e.sets() == null
                                  ? List.of()
                                  : e.sets().stream()
                                      .map(
                                          s ->
                                              new PerformedSession.PerformedSet(
                                                  s.id(),
                                                  s.setNumber(),
                                                  s.setType(),
                                                  s.side(),
                                                  s.reps(),
                                                  s.loadKg(),
                                                  s.durationSeconds(),
                                                  s.distanceM(),
                                                  s.rpe(),
                                                  s.rir(),
                                                  s.completed(),
                                                  s.completedAt()))
                                      .toList()))
                  .toList(),
          feedback == null
              ? null
              : new PerformedSession.Feedback(
                  feedback.effort(),
                  feedback.comment(),
                  feedback.pains() == null
                      ? List.of()
                      : feedback.pains().stream()
                          .map(
                              p ->
                                  new PerformedSession.Pain(
                                      p.id(),
                                      p.bodyRegion(),
                                      p.exerciseId(),
                                      p.intensity(),
                                      p.description()))
                          .toList()));
    }
  }

  @Schema(name = "SessionSync")
  record PushRequest(
      @Schema(
              requiredMode = REQUIRED,
              description =
                  "performed_by sai da conta: aluno = client; personal (presencial) = professional")
          @NotNull
          @Size(max = 50)
          List<@Valid SessionDto> sessions) {}

  @Schema(name = "SessionSyncResult")
  record PushResponse(
      @Schema(requiredMode = REQUIRED, description = "Gravadas agora") List<UUID> written,
      @Schema(
              requiredMode = REQUIRED,
              description = "Já estavam iguais ou mais novas no servidor (marque como enviadas)")
          List<UUID> unchanged) {}

  @PostMapping(
      path = "/v1/sync",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      operationId = "pushSessions",
      summary = "Envia treinos registrados no aparelho",
      description =
          "Idempotente: reenviar não duplica. Vence a edição mais recente no aparelho"
              + " (clientUpdatedAt); a sessão vai inteira e substitui a gravada.")
  @ApiResponse(responseCode = "200", description = "Resultado por sessão")
  @ApiResponse(
      responseCode = "404",
      description = "`resource-not-found` (vínculo de outra pessoa)",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
              schema = @Schema(implementation = ApiProblem.class)))
  @ApiResponse(
      responseCode = "422",
      description = "Sessão fora da regra (`session-invalid`, `set-invalid`, `pain-invalid`…)",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
              schema = @Schema(implementation = ApiProblem.class)))
  PushResponse push(@Valid @RequestBody PushRequest request) {
    var userId = currentAppUser.id().orElseThrow(Forbidden::new);
    // performed_by sai da conta no caso de uso; aqui vai o padrão (aluno)
    var result =
        sync.push(
            userId, request.sessions().stream().map(s -> s.toDomain(PerformedBy.CLIENT)).toList());
    return new PushResponse(result.written(), result.unchanged());
  }
}
