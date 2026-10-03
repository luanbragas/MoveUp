package br.com.moveup.training.application.usecase;

import br.com.moveup.accounts.api.AccountDirectory;
import br.com.moveup.shared.domain.Forbidden;
import br.com.moveup.shared.domain.IdGenerator;
import br.com.moveup.shared.domain.ResourceNotFound;
import br.com.moveup.training.application.port.in.ManageExercises;
import br.com.moveup.training.application.port.out.Exercises;
import br.com.moveup.training.domain.exception.InvalidTrainingData;
import br.com.moveup.training.domain.exception.TrainingConflict;
import br.com.moveup.training.domain.model.Exercise;
import br.com.moveup.training.domain.model.Muscle;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class ManageExercisesUseCase implements ManageExercises {

  private static final int DEFAULT_LIMIT = 50;

  private final AccountDirectory accounts;
  private final Exercises exercises;
  private final IdGenerator ids;

  public ManageExercisesUseCase(AccountDirectory accounts, Exercises exercises, IdGenerator ids) {
    this.accounts = accounts;
    this.exercises = exercises;
    this.ids = ids;
  }

  @Override
  @Transactional(readOnly = true)
  public List<ExerciseView> search(UUID professionalId, String query, String muscle, int limit) {
    var organizationId = organizationOf(professionalId);
    var size = limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_RESULTS);
    var filter = muscle == null || muscle.isBlank() ? null : parseMuscle(muscle);
    return exercises.search(organizationId, query == null ? "" : query.strip(), filter, size);
  }

  @Override
  @Transactional
  public ExerciseView create(UUID professionalId, NewExercise input) {
    var organizationId = organizationOf(professionalId);
    var exercise =
        Exercise.custom(
            ids.newId(),
            organizationId,
            input.name(),
            Exercise.Modality.fromCode(input.modality()),
            Exercise.TrackingType.fromCode(input.trackingType()),
            input.primaryMuscle() == null || input.primaryMuscle().isBlank()
                ? null
                : parseMuscle(input.primaryMuscle()),
            input.secondaryMuscles() == null
                ? List.of()
                : input.secondaryMuscles().stream()
                    .map(ManageExercisesUseCase::parseMuscle)
                    .toList(),
            input.equipment(),
            input.unilateral(),
            input.instructions(),
            input.mediaUrl());
    if (exercises.nameTaken(organizationId, exercise.name())) {
      throw TrainingConflict.exerciseNameTaken();
    }
    exercises.insert(exercise, professionalId);
    return view(exercise);
  }

  @Override
  @Transactional
  public void archive(UUID professionalId, UUID exerciseId) {
    var organizationId = organizationOf(professionalId);
    var exercise = exercises.find(exerciseId).orElseThrow(ResourceNotFound::new);
    exercise.requireEditableBy(organizationId);
    exercises.archive(exerciseId);
  }

  private UUID organizationOf(UUID professionalId) {
    return accounts.organizationOf(professionalId).orElseThrow(Forbidden::new);
  }

  private static Muscle parseMuscle(String code) {
    return Muscle.fromCode(code)
        .orElseThrow(() -> new InvalidTrainingData("muscle-invalid", "Músculo inválido."));
  }

  static ExerciseView view(Exercise e) {
    return new ExerciseView(
        e.id(),
        e.name(),
        e.modality().code(),
        e.trackingType().code(),
        e.primaryMuscle() == null ? null : e.primaryMuscle().code(),
        e.secondaryMuscles().stream().map(Muscle::code).toList(),
        e.equipment(),
        e.unilateral(),
        e.instructions(),
        e.mediaUrl(),
        !e.isBase());
  }
}
