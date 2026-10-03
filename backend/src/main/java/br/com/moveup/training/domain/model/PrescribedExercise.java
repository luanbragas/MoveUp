package br.com.moveup.training.domain.model;

import br.com.moveup.training.domain.exception.InvalidTrainingData;
import java.util.List;
import java.util.UUID;

/** Exercício dentro de um bloco, com as séries e a orientação para este aluno. */
public record PrescribedExercise(
    UUID exerciseId, Integer restSeconds, String notes, List<PrescribedSet> sets) {

  public static final int MAX_SETS = 20;
  public static final int MAX_NOTES = 500;

  public PrescribedExercise {
    if (exerciseId == null) {
      throw new InvalidTrainingData("exercise-required", "Escolha o exercício.");
    }
    sets = List.copyOf(sets == null ? List.of() : sets);
    if (sets.size() > MAX_SETS) {
      throw new InvalidTrainingData("sets-invalid", "No máximo 20 séries por exercício.");
    }
    if (restSeconds != null && (restSeconds < 0 || restSeconds > 60 * 60)) {
      throw new InvalidTrainingData("rest-invalid", "Descanso inválido.");
    }
    notes = notes == null || notes.isBlank() ? null : notes.strip();
    if (notes != null && notes.length() > MAX_NOTES) {
      throw new InvalidTrainingData("notes-invalid", "A orientação pode ter até 500 caracteres.");
    }
  }
}
