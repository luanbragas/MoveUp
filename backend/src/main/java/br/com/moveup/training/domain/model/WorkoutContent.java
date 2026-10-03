package br.com.moveup.training.domain.model;

import br.com.moveup.training.domain.exception.InvalidTrainingData;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** O que o aluno vai fazer: objetivo do treino, tempo estimado, observações e os blocos. */
public record WorkoutContent(
    String goal, Integer estimatedMinutes, String notes, List<WorkoutBlock> blocks) {

  public static final int MAX_BLOCKS = 20;

  public WorkoutContent {
    blocks = List.copyOf(blocks == null ? List.of() : blocks);
    goal = goal == null || goal.isBlank() ? null : goal.strip();
    notes = notes == null || notes.isBlank() ? null : notes.strip();
    if (goal != null && goal.length() > 120) {
      throw new InvalidTrainingData(
          "goal-invalid", "O objetivo do treino pode ter até 120 letras.");
    }
    if (notes != null && notes.length() > 1000) {
      throw new InvalidTrainingData("notes-invalid", "As observações podem ter até 1000 letras.");
    }
    if (estimatedMinutes != null && (estimatedMinutes <= 0 || estimatedMinutes > 600)) {
      throw new InvalidTrainingData("duration-invalid", "Tempo estimado inválido.");
    }
    if (blocks.size() > MAX_BLOCKS) {
      throw new InvalidTrainingData("workout-too-big", "No máximo 20 blocos por treino.");
    }
  }

  public static WorkoutContent empty() {
    return new WorkoutContent(null, null, null, List.of());
  }

  /** Exercícios usados, sem repetir, na ordem em que aparecem. */
  public Set<UUID> exerciseIds() {
    var ids = new LinkedHashSet<UUID>();
    blocks.forEach(b -> b.exercises().forEach(e -> ids.add(e.exerciseId())));
    return ids;
  }
}
