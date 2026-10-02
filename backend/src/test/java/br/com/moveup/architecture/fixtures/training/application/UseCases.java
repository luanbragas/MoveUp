package br.com.moveup.architecture.fixtures.training.application;

import br.com.moveup.architecture.fixtures.training.infrastructure.persistence.WorkoutRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Violação: caso de uso usando o adaptador direto. */
class LeakyUseCase {
  WorkoutRepository repository;
}

/** Violação: caso de uso como bean do Spring. */
@Service
class AnnotatedUseCase {}

/** Permitido: só @Transactional. */
class TransactionalUseCase {
  @Transactional
  void run() {}
}
