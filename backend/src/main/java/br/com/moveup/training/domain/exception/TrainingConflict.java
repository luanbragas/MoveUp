package br.com.moveup.training.domain.exception;

import br.com.moveup.shared.domain.ConflictException;

/** Regra do estado atual: nome repetido na biblioteca, exercício da base, treino arquivado. */
public final class TrainingConflict extends ConflictException {

  public static final String EXERCISE_NAME_TAKEN = "exercise-name-taken";
  public static final String BASE_EXERCISE_READ_ONLY = "base-exercise-read-only";

  private TrainingConflict(String code, String safeMessage) {
    super(code, safeMessage);
  }

  public static TrainingConflict exerciseNameTaken() {
    return new TrainingConflict(
        EXERCISE_NAME_TAKEN, "Já existe um exercício com esse nome na sua biblioteca.");
  }

  public static TrainingConflict baseExerciseReadOnly() {
    return new TrainingConflict(
        BASE_EXERCISE_READ_ONLY, "Exercícios da biblioteca base não podem ser alterados.");
  }
}
