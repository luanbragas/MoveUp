package br.com.moveup.training.domain.model;

import br.com.moveup.training.domain.exception.InvalidTrainingData;
import java.util.List;
import java.util.Locale;

/**
 * Bloco do treino: um método (sequencial, biset, circuito, HIIT, EMOM, AMRAP, intervalado) com os
 * exercícios. Cada método exige os próprios tempos; Tabata é o HIIT 20 s/10 s × 8.
 */
public record WorkoutBlock(
    String name,
    Method method,
    String preset,
    Integer rounds,
    Integer workSeconds,
    Integer restSeconds,
    Integer restBetweenRounds,
    Integer durationSeconds,
    String notes,
    List<PrescribedExercise> exercises) {

  public static final int MAX_EXERCISES = 12;
  public static final String TABATA = "tabata";

  public enum Method {
    SEQUENTIAL,
    SUPERSET,
    CIRCUIT,
    HIIT,
    EMOM,
    AMRAP,
    INTERVALS;

    public String code() {
      return name().toLowerCase(Locale.ROOT);
    }

    public static Method fromCode(String code) {
      for (var value : values()) {
        if (value.code().equals(code)) {
          return value;
        }
      }
      throw new InvalidTrainingData("method-invalid", "Método de bloco inválido.");
    }
  }

  public WorkoutBlock {
    if (method == null) {
      throw new InvalidTrainingData("method-invalid", "Escolha o método do bloco.");
    }
    exercises = List.copyOf(exercises == null ? List.of() : exercises);
    name = name == null || name.isBlank() ? null : name.strip();
    notes = notes == null || notes.isBlank() ? null : notes.strip();
    preset = preset == null || preset.isBlank() ? null : preset.strip().toLowerCase(Locale.ROOT);
    if (name != null && name.length() > 80) {
      throw new InvalidTrainingData(
          "block-name-invalid", "O nome do bloco pode ter até 80 letras.");
    }
    if (exercises.isEmpty()) {
      throw new InvalidTrainingData("block-empty", "Adicione pelo menos um exercício no bloco.");
    }
    if (exercises.size() > MAX_EXERCISES) {
      throw new InvalidTrainingData("block-too-big", "No máximo 12 exercícios por bloco.");
    }
    positive(rounds, 100, "rounds-invalid", "Rodadas inválidas.");
    positive(workSeconds, 3600, "duration-invalid", "Tempo de trabalho inválido.");
    nonNegative(restSeconds, "rest-invalid", "Descanso inválido.");
    nonNegative(restBetweenRounds, "rest-invalid", "Descanso entre rodadas inválido.");
    positive(durationSeconds, 4 * 3600, "duration-invalid", "Duração inválida.");

    if (preset != null) {
      if (!TABATA.equals(preset) || method != Method.HIIT) {
        throw new InvalidTrainingData("preset-invalid", "Preset só existe para HIIT (Tabata).");
      }
      // Tabata: sempre 20 s de esforço, 10 s de descanso, 8 rodadas
      rounds = 8;
      workSeconds = 20;
      restSeconds = 10;
    }
    switch (method) {
      case SEQUENTIAL -> requireSets(exercises);
      case SUPERSET -> {
        if (exercises.size() < 2) {
          throw new InvalidTrainingData(
              "superset-needs-two", "Biset precisa de pelo menos dois exercícios.");
        }
        requireSets(exercises);
      }
      case CIRCUIT -> {
        if (exercises.size() < 2) {
          throw new InvalidTrainingData(
              "circuit-needs-two", "Circuito precisa de pelo menos dois exercícios.");
        }
        require(rounds, "rounds-required", "Informe quantas rodadas.");
      }
      case HIIT, INTERVALS -> {
        require(rounds, "rounds-required", "Informe quantas rodadas.");
        require(workSeconds, "work-required", "Informe o tempo de esforço.");
        require(restSeconds, "rest-required", "Informe o tempo de descanso.");
      }
      case EMOM, AMRAP -> require(durationSeconds, "duration-required", "Informe a duração.");
    }
  }

  private static void requireSets(List<PrescribedExercise> exercises) {
    if (exercises.stream().anyMatch(e -> e.sets().isEmpty())) {
      throw new InvalidTrainingData("sets-required", "Todo exercício precisa de séries.");
    }
  }

  private static void require(Integer value, String code, String message) {
    if (value == null) {
      throw new InvalidTrainingData(code, message);
    }
  }

  private static void positive(Integer value, int max, String code, String message) {
    if (value != null && (value <= 0 || value > max)) {
      throw new InvalidTrainingData(code, message);
    }
  }

  private static void nonNegative(Integer value, String code, String message) {
    if (value != null && (value < 0 || value > 3600)) {
      throw new InvalidTrainingData(code, message);
    }
  }
}
