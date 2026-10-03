package br.com.moveup.training.application.port.out;

import br.com.moveup.training.application.port.in.ManageWorkouts.WorkoutSummary;
import br.com.moveup.training.domain.model.Workout;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Treinos e versões (conteúdo em workout_version → workout_block → prescribed_*). */
public interface Workouts {

  /** Treino com o conteúdo da versão atual. */
  Optional<Workout> find(UUID workoutId);

  /** Treino novo com a versão 1. */
  void insert(Workout workout, UUID createdBy);

  /** Grava o resultado de {@link Workout#edit}: versão nova ou conteúdo da atual substituído. */
  void saveEdit(Workout workout, boolean newVersion, UUID editedBy);

  void saveArchive(Workout workout);

  List<WorkoutSummary> templates(UUID organizationId);
}
