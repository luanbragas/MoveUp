package br.com.moveup.architecture.fixtures.execution.infrastructure;

import br.com.moveup.architecture.fixtures.training.api.TrainingQuery;
import br.com.moveup.architecture.fixtures.training.domain.Workout;

/** Violação: outro módulo acessando o domínio de training. */
class ReachesIntoTrainingDomain {
  Workout workout;
}

/** Permitido: outro módulo usando a api de training. */
class UsesTrainingApi {
  TrainingQuery query;
}
