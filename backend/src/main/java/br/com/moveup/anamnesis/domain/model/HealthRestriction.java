package br.com.moveup.anamnesis.domain.model;

import br.com.moveup.anamnesis.domain.exception.InvalidAnamnesisData;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * Lesão, cirurgia, dor ou condição que gera aviso ao montar treino. Ativa enquanto não tem data de
 * resolução. A descrição é dado de saúde (vai cifrada).
 */
public record HealthRestriction(
    UUID id,
    UUID clientId,
    String kind,
    String bodyRegion,
    String description,
    Integer severity,
    LocalDate resolvedOn,
    UUID sourceAnamnesisId) {

  static final Set<String> KINDS = Set.of("injury", "surgery", "pain", "condition");

  /** Mesma lista do relato de dor e do mapa do app. */
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

  public HealthRestriction {
    if (!KINDS.contains(kind)) {
      throw new InvalidAnamnesisData("restriction-kind-invalid", "Tipo de restrição inválido.");
    }
    if (bodyRegion != null && !BODY_REGIONS.contains(bodyRegion)) {
      throw new InvalidAnamnesisData("restriction-region-invalid", "Região do corpo inválida.");
    }
    description = description == null ? "" : description.strip();
    if (description.isEmpty() || description.length() > 500) {
      throw new InvalidAnamnesisData(
          "restriction-description-invalid", "Descreva a restrição em até 500 caracteres.");
    }
    if (severity != null && (severity < 1 || severity > 3)) {
      throw new InvalidAnamnesisData("restriction-severity-invalid", "Gravidade de 1 a 3.");
    }
  }

  public boolean active() {
    return resolvedOn == null;
  }
}
