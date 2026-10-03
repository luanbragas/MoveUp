package br.com.moveup.training.application.usecase;

import br.com.moveup.accounts.api.AccountDirectory;
import br.com.moveup.execution.api.PlannedVersionUsage;
import br.com.moveup.shared.domain.Forbidden;
import br.com.moveup.shared.domain.IdGenerator;
import br.com.moveup.shared.domain.ResourceNotFound;
import br.com.moveup.training.application.port.in.ManageWorkouts;
import br.com.moveup.training.application.port.out.ExerciseCatalog;
import br.com.moveup.training.application.port.out.Workouts;
import br.com.moveup.training.domain.model.Workout;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class ManageWorkoutsUseCase implements ManageWorkouts {

  private final AccountDirectory accounts;
  private final Workouts workouts;
  private final ExerciseCatalog catalog;
  private final PlannedVersionUsage versionUsage;
  private final IdGenerator ids;

  public ManageWorkoutsUseCase(
      AccountDirectory accounts,
      Workouts workouts,
      ExerciseCatalog catalog,
      PlannedVersionUsage versionUsage,
      IdGenerator ids) {
    this.accounts = accounts;
    this.workouts = workouts;
    this.catalog = catalog;
    this.versionUsage = versionUsage;
    this.ids = ids;
  }

  @Override
  @Transactional
  public WorkoutView createTemplate(UUID professionalId, String name, ContentInput input) {
    var organizationId = organizationOf(professionalId);
    var content = WorkoutMapping.content(input);
    var refs = catalog.refs(organizationId, content.exerciseIds());
    WorkoutMapping.requireKnown(content, refs);
    var workout = Workout.newTemplate(ids.newId(), ids.newId(), organizationId, name, content);
    workouts.insert(workout, professionalId);
    return WorkoutMapping.view(workout, refs);
  }

  @Override
  @Transactional(readOnly = true)
  public List<WorkoutSummary> listTemplates(UUID professionalId) {
    return workouts.templates(organizationOf(professionalId));
  }

  @Override
  @Transactional(readOnly = true)
  public WorkoutView get(UUID professionalId, UUID workoutId) {
    var organizationId = organizationOf(professionalId);
    var workout = owned(workoutId, organizationId);
    return WorkoutMapping.view(
        workout, catalog.refs(organizationId, workout.content().exerciseIds()));
  }

  @Override
  @Transactional
  public WorkoutView save(
      UUID professionalId, UUID workoutId, int expectedRevision, String name, ContentInput input) {
    var organizationId = organizationOf(professionalId);
    var workout = owned(workoutId, organizationId);
    var content = WorkoutMapping.content(input);
    var refs = catalog.refs(organizationId, content.exerciseIds());
    WorkoutMapping.requireKnown(content, refs);
    var used = !workout.isTemplate() && versionUsage.isUsed(workout.versionId());
    var newVersion = workout.edit(name, content, expectedRevision, used, ids.newId());
    workouts.saveEdit(workout, newVersion, professionalId);
    return WorkoutMapping.view(workout, refs);
  }

  @Override
  @Transactional
  public void archive(UUID professionalId, UUID workoutId, int expectedRevision) {
    var workout = owned(workoutId, organizationOf(professionalId));
    workout.archive(expectedRevision);
    workouts.saveArchive(workout);
  }

  private Workout owned(UUID workoutId, UUID organizationId) {
    var workout = workouts.find(workoutId).orElseThrow(ResourceNotFound::new);
    workout.requireOwnedBy(organizationId);
    return workout;
  }

  private UUID organizationOf(UUID professionalId) {
    return accounts.organizationOf(professionalId).orElseThrow(Forbidden::new);
  }
}
