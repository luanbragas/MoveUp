package br.com.moveup.training.domain.model;

import br.com.moveup.shared.domain.ResourceNotFound;
import br.com.moveup.training.domain.exception.InvalidTrainingData;
import br.com.moveup.training.domain.exception.TrainingConflict;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Exercício da biblioteca. {@code organizationId} nulo = biblioteca base (todos veem, ninguém edita
 * pelo app); senão, exercício próprio da organização do personal.
 */
public record Exercise(
    UUID id,
    UUID organizationId,
    String name,
    Modality modality,
    TrackingType trackingType,
    Muscle primaryMuscle,
    List<Muscle> secondaryMuscles,
    String equipment,
    boolean unilateral,
    String instructions,
    String mediaUrl) {

  public static final int MAX_NAME = 120;
  public static final int MAX_INSTRUCTIONS = 2000;
  public static final int MAX_SECONDARY = 6;

  public enum Modality {
    STRENGTH,
    CARDIO,
    CONDITIONING,
    COMPLEMENTARY;

    public String code() {
      return name().toLowerCase(Locale.ROOT);
    }

    public static Modality fromCode(String code) {
      for (var value : values()) {
        if (value.code().equals(code)) {
          return value;
        }
      }
      throw new InvalidTrainingData("modality-invalid", "Modalidade inválida.");
    }
  }

  /** O que o aluno registra em cada série. */
  public enum TrackingType {
    REPS_LOAD,
    REPS_ONLY,
    TIME,
    DISTANCE_TIME;

    public String code() {
      return name().toLowerCase(Locale.ROOT);
    }

    public static TrackingType fromCode(String code) {
      for (var value : values()) {
        if (value.code().equals(code)) {
          return value;
        }
      }
      throw new InvalidTrainingData("tracking-type-invalid", "Tipo de registro inválido.");
    }
  }

  public Exercise {
    secondaryMuscles = List.copyOf(secondaryMuscles);
  }

  /** Exercício próprio do personal, com as regras de cadastro. */
  public static Exercise custom(
      UUID id,
      UUID organizationId,
      String name,
      Modality modality,
      TrackingType trackingType,
      Muscle primaryMuscle,
      List<Muscle> secondaryMuscles,
      String equipment,
      boolean unilateral,
      String instructions,
      String mediaUrl) {
    var cleanName = name == null ? "" : name.strip().replaceAll("\\s+", " ");
    if (cleanName.length() < 2 || cleanName.length() > MAX_NAME) {
      throw new InvalidTrainingData(
          "exercise-name-invalid", "O nome do exercício precisa ter de 2 a 120 caracteres.");
    }
    var secondary = Set.copyOf(secondaryMuscles);
    if (secondary.size() > MAX_SECONDARY
        || (primaryMuscle != null && secondary.contains(primaryMuscle))) {
      throw new InvalidTrainingData(
          "muscle-invalid", "Músculos secundários repetidos ou iguais ao principal.");
    }
    var cleanInstructions = blankToNull(instructions);
    if (cleanInstructions != null && cleanInstructions.length() > MAX_INSTRUCTIONS) {
      throw new InvalidTrainingData(
          "instructions-invalid", "As instruções podem ter até 2000 caracteres.");
    }
    return new Exercise(
        id,
        organizationId,
        cleanName,
        modality,
        trackingType,
        primaryMuscle,
        List.copyOf(secondary),
        blankToNull(equipment),
        unilateral,
        cleanInstructions,
        validMediaUrl(mediaUrl));
  }

  public boolean isBase() {
    return organizationId == null;
  }

  /** Só o dono arquiva; os da base são de todos e não mudam pelo app. */
  public void requireEditableBy(UUID organization) {
    if (isBase()) {
      throw TrainingConflict.baseExerciseReadOnly();
    }
    if (!organizationId.equals(organization)) {
      throw new ResourceNotFound();
    }
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  /** Vídeo é link externo (ex.: YouTube) e precisa ser https. */
  private static String validMediaUrl(String value) {
    var url = blankToNull(value);
    if (url == null) {
      return null;
    }
    try {
      var uri = URI.create(url);
      if ("https".equals(uri.getScheme()) && uri.getHost() != null && url.length() <= 500) {
        return url;
      }
    } catch (IllegalArgumentException e) {
      // cai no erro abaixo
    }
    throw new InvalidTrainingData("media-url-invalid", "O vídeo precisa ser um link https.");
  }
}
