package br.com.moveup.training.domain.model;

import br.com.moveup.training.domain.exception.InvalidTrainingData;
import java.math.BigDecimal;
import java.util.Locale;

/**
 * Uma série planejada. Faixa de repetições (8 a 12 = min 8, max 12), carga em kg, tempo ou
 * distância conforme o exercício, esforço alvo (RPE ou RIR) e descanso depois dela.
 */
public record PrescribedSet(
    SetType type,
    Integer repsMin,
    Integer repsMax,
    BigDecimal loadKg,
    Integer durationSeconds,
    Integer distanceM,
    BigDecimal targetRpe,
    Integer targetRir,
    Integer restSeconds) {

  public static final int MAX_REPS = 500;
  public static final BigDecimal MAX_LOAD = new BigDecimal("1000");
  public static final int MAX_SECONDS = 4 * 60 * 60;

  public enum SetType {
    WARMUP,
    NORMAL,
    DROP,
    REST_PAUSE,
    FAILURE;

    public String code() {
      return name().toLowerCase(Locale.ROOT);
    }

    public static SetType fromCode(String code) {
      for (var value : values()) {
        if (value.code().equals(code)) {
          return value;
        }
      }
      throw new InvalidTrainingData("set-type-invalid", "Tipo de série inválido.");
    }
  }

  public PrescribedSet {
    if (type == null) {
      type = SetType.NORMAL;
    }
    requireRange(repsMin, 0, MAX_REPS, "reps-invalid", "Repetições fora do intervalo.");
    requireRange(repsMax, 1, MAX_REPS, "reps-invalid", "Repetições fora do intervalo.");
    if (repsMin != null && repsMax != null && repsMin > repsMax) {
      throw new InvalidTrainingData(
          "reps-invalid", "O mínimo de repetições não pode passar do máximo.");
    }
    if (loadKg != null && (loadKg.signum() < 0 || loadKg.compareTo(MAX_LOAD) > 0)) {
      throw new InvalidTrainingData("load-invalid", "Carga fora do intervalo.");
    }
    if (loadKg != null && loadKg.scale() > 3) {
      throw new InvalidTrainingData("load-invalid", "Carga com casas decimais demais.");
    }
    requireRange(durationSeconds, 1, MAX_SECONDS, "duration-invalid", "Tempo inválido.");
    requireRange(distanceM, 1, 100_000, "distance-invalid", "Distância inválida.");
    if (targetRpe != null
        && (targetRpe.signum() < 0
            || targetRpe.compareTo(BigDecimal.TEN) > 0
            || targetRpe.scale() > 1)) {
      throw new InvalidTrainingData("effort-invalid", "RPE vai de 0 a 10.");
    }
    requireRange(targetRir, 0, 10, "effort-invalid", "RIR vai de 0 a 10.");
    if (targetRpe != null && targetRir != null) {
      throw new InvalidTrainingData("effort-invalid", "Use RPE ou RIR, não os dois.");
    }
    requireRange(restSeconds, 0, 60 * 60, "rest-invalid", "Descanso inválido.");
  }

  private static void requireRange(Integer value, int min, int max, String code, String message) {
    if (value != null && (value < min || value > max)) {
      throw new InvalidTrainingData(code, message);
    }
  }
}
