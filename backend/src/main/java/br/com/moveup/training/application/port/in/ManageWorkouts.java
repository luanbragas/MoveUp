package br.com.moveup.training.application.port.in;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Treinos do personal: modelos e treinos de programa, editados por inteiro no "Salvar" do editor
 * com concorrência otimista (revisão = ETag).
 */
public interface ManageWorkouts {

  WorkoutView createTemplate(UUID professionalId, String name, ContentInput content);

  List<WorkoutSummary> listTemplates(UUID professionalId);

  WorkoutView get(UUID professionalId, UUID workoutId);

  /**
   * @param expectedRevision revisão que o app editou (If-Match); diferente = version-mismatch
   */
  WorkoutView save(
      UUID professionalId, UUID workoutId, int expectedRevision, String name, ContentInput content);

  void archive(UUID professionalId, UUID workoutId, int expectedRevision);

  // ---------------------------------------------------------------- entrada

  record ContentInput(
      String goal, Integer estimatedMinutes, String notes, List<BlockInput> blocks) {}

  record BlockInput(
      String name,
      String method,
      String preset,
      Integer rounds,
      Integer workSeconds,
      Integer restSeconds,
      Integer restBetweenRounds,
      Integer durationSeconds,
      String notes,
      List<ExerciseInput> exercises) {}

  record ExerciseInput(UUID exerciseId, Integer restSeconds, String notes, List<SetInput> sets) {}

  record SetInput(
      String type,
      Integer repsMin,
      Integer repsMax,
      BigDecimal loadKg,
      Integer durationSeconds,
      Integer distanceM,
      BigDecimal targetRpe,
      Integer targetRir,
      Integer restSeconds) {}

  // ---------------------------------------------------------------- saída

  /**
   * @param programId nulo em modelo
   * @param versionNumber versão do conteúdo (sobe quando o treino já tinha sessão)
   */
  record WorkoutView(
      UUID id,
      String name,
      boolean template,
      UUID programId,
      UUID sourceTemplateId,
      int revision,
      int versionNumber,
      ContentView content) {}

  record ContentView(String goal, Integer estimatedMinutes, String notes, List<BlockView> blocks) {}

  record BlockView(
      String name,
      String method,
      String preset,
      Integer rounds,
      Integer workSeconds,
      Integer restSeconds,
      Integer restBetweenRounds,
      Integer durationSeconds,
      String notes,
      List<ExerciseView> exercises) {}

  /** O exercício vem com nome e tipo de registro para o editor não precisar buscar de novo. */
  record ExerciseView(
      UUID exerciseId,
      String exerciseName,
      String trackingType,
      String primaryMuscle,
      Integer restSeconds,
      String notes,
      List<SetInput> sets) {}

  record WorkoutSummary(
      UUID id,
      String name,
      int exercises,
      int blocks,
      Integer estimatedMinutes,
      Instant updatedAt) {}
}
