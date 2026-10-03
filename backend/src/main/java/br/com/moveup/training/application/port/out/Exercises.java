package br.com.moveup.training.application.port.out;

import br.com.moveup.training.application.port.in.ManageExercises.ExerciseView;
import br.com.moveup.training.domain.model.Exercise;
import br.com.moveup.training.domain.model.Muscle;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Exercícios da base (organização nula) e da organização do personal. */
public interface Exercises {

  /**
   * @param query texto sem tratamento; vazio = todos em ordem alfabética
   * @param muscle principal ou secundário; nulo = qualquer
   */
  List<ExerciseView> search(UUID organizationId, String query, Muscle muscle, int limit);

  Optional<Exercise> find(UUID exerciseId);

  /** Já existe exercício ativo com esse nome (sem acento e caixa) na organização ou na base. */
  boolean nameTaken(UUID organizationId, String name);

  void insert(Exercise exercise, UUID createdBy);

  void archive(UUID exerciseId);
}
