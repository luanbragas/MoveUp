package br.com.moveup.execution.api;

import java.util.UUID;

/**
 * Para o módulo training: uma versão de treino já foi usada numa sessão (feita ou em andamento)? Se
 * sim, editar o treino cria versão nova, e a sessão continua comparando com o planejado dela.
 */
public interface PlannedVersionUsage {

  boolean isUsed(UUID workoutVersionId);
}
