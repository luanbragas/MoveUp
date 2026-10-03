package br.com.moveup.training.application.port.out;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** O que o treino precisa saber dos exercícios: se a organização pode usar, nome e registro. */
public interface ExerciseCatalog {

  /**
   * Exercícios da base ou da organização (inclusive arquivados: treino antigo continua válido),
   * pelo id.
   */
  Map<UUID, ExerciseRef> refs(UUID organizationId, Collection<UUID> exerciseIds);

  record ExerciseRef(String name, String trackingType, String primaryMuscle) {}
}
