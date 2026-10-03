package br.com.moveup.execution.domain.model;

import br.com.moveup.execution.domain.exception.InvalidSessionData;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Treino realizado, como o app registrou offline: a sessão inteira (exercícios, séries, feedback e
 * dores) com ids gerados no aparelho e a hora da última edição lá ({@code clientUpdatedAt}). O sync
 * é idempotente: vence a edição mais recente no aparelho.
 */
public record PerformedSession(
    UUID id,
    UUID linkId,
    UUID programId,
    UUID workoutId,
    UUID workoutVersionId,
    Status status,
    Instant startedAt,
    Instant finishedAt,
    Integer durationSeconds,
    BigDecimal completionRatio,
    PerformedBy performedBy,
    Instant clientUpdatedAt,
    List<Exercise> exercises,
    List<BlockResult> blockResults,
    Feedback feedback) {

  public static final int MAX_EXERCISES = 40;
  public static final int MAX_SETS = 30;

  public enum Status {
    IN_PROGRESS,
    COMPLETED,
    PARTIAL,
    ABANDONED;

    public String code() {
      return name().toLowerCase(Locale.ROOT);
    }

    public boolean finished() {
      return this == COMPLETED || this == PARTIAL;
    }

    public static Status fromCode(String code) {
      for (var value : values()) {
        if (value.code().equals(code)) {
          return value;
        }
      }
      throw new InvalidSessionData("session-status-invalid", "Status da sessão inválido.");
    }
  }

  public enum PerformedBy {
    CLIENT,
    PROFESSIONAL;

    public String code() {
      return name().toLowerCase(Locale.ROOT);
    }
  }

  /** Regiões do relato de dor (lista controlada pelo app, ARQUITETURA 5). */
  public static final Set<String> BODY_REGIONS =
      Set.of(
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
          "other");

  public record PerformedSet(
      UUID id,
      int setNumber,
      String setType,
      String side,
      Integer reps,
      BigDecimal loadKg,
      Integer durationSeconds,
      Integer distanceM,
      BigDecimal rpe,
      Integer rir,
      boolean completed,
      Instant completedAt) {

    private static final Set<String> TYPES =
        Set.of("warmup", "normal", "drop", "rest_pause", "failure");

    public PerformedSet {
      if (id == null || setNumber < 1) {
        throw new InvalidSessionData("set-invalid", "Série sem id ou número.");
      }
      setType = setType == null ? "normal" : setType;
      if (!TYPES.contains(setType)) {
        throw new InvalidSessionData("set-type-invalid", "Tipo de série inválido.");
      }
      if (side != null && !side.equals("left") && !side.equals("right")) {
        throw new InvalidSessionData("set-invalid", "Lado inválido.");
      }
      if ((reps != null && (reps < 0 || reps > 1000))
          || (loadKg != null
              && (loadKg.signum() < 0 || loadKg.compareTo(new BigDecimal("2000")) > 0))
          || (durationSeconds != null && (durationSeconds < 0 || durationSeconds > 24 * 3600))
          || (distanceM != null && (distanceM < 0 || distanceM > 1_000_000))
          || (rpe != null && (rpe.signum() < 0 || rpe.compareTo(BigDecimal.TEN) > 0))
          || (rir != null && (rir < 0 || rir > 10))) {
        throw new InvalidSessionData("set-invalid", "Valor da série fora do intervalo.");
      }
    }
  }

  public record Exercise(
      UUID id,
      UUID exerciseId,
      int position,
      String status,
      UUID substitutedFrom,
      String notes,
      List<PerformedSet> sets) {

    public Exercise {
      if (id == null || exerciseId == null || position < 1) {
        throw new InvalidSessionData("exercise-invalid", "Exercício sem id ou posição.");
      }
      if (!Set.of("done", "skipped", "substituted").contains(status)) {
        throw new InvalidSessionData("exercise-invalid", "Status do exercício inválido.");
      }
      if (("substituted".equals(status)) != (substitutedFrom != null)) {
        throw new InvalidSessionData(
            "exercise-invalid", "Exercício trocado precisa dizer qual era o original.");
      }
      sets = List.copyOf(sets == null ? List.of() : sets);
      if (sets.size() > MAX_SETS) {
        throw new InvalidSessionData("exercise-invalid", "Séries demais.");
      }
      notes = notes == null || notes.isBlank() ? null : notes.strip();
      if (notes != null && notes.length() > 500) {
        throw new InvalidSessionData("notes-invalid", "Anotação longa demais.");
      }
    }
  }

  public record Pain(
      UUID id, String bodyRegion, UUID exerciseId, Integer intensity, String description) {

    public Pain {
      if (id == null || !BODY_REGIONS.contains(bodyRegion)) {
        throw new InvalidSessionData("pain-invalid", "Região da dor inválida.");
      }
      if (intensity != null && (intensity < 0 || intensity > 10)) {
        throw new InvalidSessionData("pain-invalid", "Intensidade vai de 0 a 10.");
      }
      description = description == null || description.isBlank() ? null : description.strip();
      if (description != null && description.length() > 500) {
        throw new InvalidSessionData("pain-invalid", "Descrição longa demais.");
      }
    }
  }

  /**
   * Resultado de um bloco por tempo (HIIT, intervalado, EMOM, AMRAP): rodadas completas, reps a
   * mais na rodada incompleta e o tempo total. O bloco é a posição dele na versão do treino.
   */
  public record BlockResult(
      int blockIndex, Integer roundsCompleted, Integer extraReps, Integer totalSeconds) {

    public BlockResult {
      if (blockIndex < 0 || blockIndex >= MAX_EXERCISES) {
        throw new InvalidSessionData("block-result-invalid", "Bloco inválido.");
      }
      if ((roundsCompleted != null && (roundsCompleted < 0 || roundsCompleted > 1000))
          || (extraReps != null && (extraReps < 0 || extraReps > 1000))
          || (totalSeconds != null && (totalSeconds < 0 || totalSeconds > 24 * 3600))) {
        throw new InvalidSessionData(
            "block-result-invalid", "Resultado do bloco fora do intervalo.");
      }
    }
  }

  /** Como foi o treino: esforço 0–10, comentário e as dores relatadas. */
  public record Feedback(int effort, String comment, List<Pain> pains) {

    public Feedback {
      if (effort < 0 || effort > 10) {
        throw new InvalidSessionData("effort-invalid", "Esforço vai de 0 a 10.");
      }
      comment = comment == null || comment.isBlank() ? null : comment.strip();
      if (comment != null && comment.length() > 1000) {
        throw new InvalidSessionData("comment-invalid", "Comentário longo demais.");
      }
      pains = List.copyOf(pains == null ? List.of() : pains);
      if (pains.size() > 10) {
        throw new InvalidSessionData("pain-invalid", "Dores demais num treino.");
      }
    }

    public boolean hasPain() {
      return !pains.isEmpty();
    }
  }

  /** Quem registrou é a conta que enviou (aluno ou personal no presencial), não o corpo. */
  public PerformedSession registeredBy(PerformedBy by) {
    return new PerformedSession(
        id,
        linkId,
        programId,
        workoutId,
        workoutVersionId,
        status,
        startedAt,
        finishedAt,
        durationSeconds,
        completionRatio,
        by,
        clientUpdatedAt,
        exercises,
        blockResults,
        feedback);
  }

  public PerformedSession {
    if (id == null || linkId == null || startedAt == null || clientUpdatedAt == null) {
      throw new InvalidSessionData("session-invalid", "Sessão sem id, vínculo ou horário.");
    }
    if ((workoutId == null) != (workoutVersionId == null)) {
      throw new InvalidSessionData(
          "session-invalid", "Treino e versão do treino vêm juntos (ou nenhum).");
    }
    if (status.finished() && finishedAt == null) {
      throw new InvalidSessionData(
          "session-invalid", "Sessão finalizada precisa do horário do fim.");
    }
    if (finishedAt != null && finishedAt.isBefore(startedAt)) {
      throw new InvalidSessionData("session-invalid", "O fim não pode ser antes do início.");
    }
    if (completionRatio != null
        && (completionRatio.signum() < 0 || completionRatio.compareTo(BigDecimal.ONE) > 0)) {
      throw new InvalidSessionData("session-invalid", "Conclusão vai de 0 a 1.");
    }
    exercises = List.copyOf(exercises == null ? List.of() : exercises);
    if (exercises.size() > MAX_EXERCISES) {
      throw new InvalidSessionData("session-invalid", "Exercícios demais na sessão.");
    }
    blockResults = List.copyOf(blockResults == null ? List.of() : blockResults);
    var blocks = new HashSet<Integer>();
    if (blockResults.stream().anyMatch(b -> !blocks.add(b.blockIndex()))) {
      throw new InvalidSessionData("block-result-invalid", "Bloco repetido na sessão.");
    }
    var ids = new HashSet<UUID>();
    for (var e : exercises) {
      if (!ids.add(e.id()) || e.sets().stream().anyMatch(s -> !ids.add(s.id()))) {
        throw new InvalidSessionData("session-invalid", "Id repetido na sessão.");
      }
    }
    if (feedback != null && feedback.pains().stream().anyMatch(p -> !ids.add(p.id()))) {
      throw new InvalidSessionData("session-invalid", "Id repetido na sessão.");
    }
  }
}
