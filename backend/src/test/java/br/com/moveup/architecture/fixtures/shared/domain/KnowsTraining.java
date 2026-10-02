package br.com.moveup.architecture.fixtures.shared.domain;

import br.com.moveup.architecture.fixtures.training.api.TrainingQuery;

/** Violação: o núcleo compartilhado conhecendo um módulo. */
class KnowsTraining {
  TrainingQuery query;
}
