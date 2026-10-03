package br.com.moveup.training.application.usecase;

import br.com.moveup.accounts.api.AccountDirectory;
import br.com.moveup.accounts.api.AccountDirectory.LinkReadiness;
import br.com.moveup.shared.domain.Forbidden;
import br.com.moveup.training.application.port.in.ManageExercises.ExerciseView;
import br.com.moveup.training.application.port.in.PlannedSync;
import br.com.moveup.training.application.port.out.Exercises;
import br.com.moveup.training.application.port.out.Programs;
import br.com.moveup.training.application.port.out.Workouts;
import br.com.moveup.training.domain.model.Program;
import br.com.moveup.training.domain.model.Workout;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class PlannedSyncUseCase implements PlannedSync {

  private final AccountDirectory accounts;
  private final Programs programs;
  private final Workouts workouts;
  private final Exercises exercises;
  private final Clock clock;

  public PlannedSyncUseCase(
      AccountDirectory accounts,
      Programs programs,
      Workouts workouts,
      Exercises exercises,
      Clock clock) {
    this.accounts = accounts;
    this.programs = programs;
    this.workouts = workouts;
    this.exercises = exercises;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public SyncPage changesSince(UUID userId, Instant since) {
    if (accounts.linkReadiness(userId) == LinkReadiness.NOT_A_CLIENT) {
      throw new Forbidden();
    }
    // o cursor novo é o início desta leitura: o que for gravado durante ela vem na próxima
    var cursor = clock.instant();
    var from = since == null ? Instant.EPOCH : since.minusSeconds(OVERLAP_SECONDS);
    var snapshots = new ArrayList<ProgramSnapshot>();
    Set<UUID> exerciseIds = new LinkedHashSet<>();
    for (var programId : programs.changedForOwnClient(from)) {
      programs
          .find(programId)
          .ifPresent(
              program -> {
                var snapshot = snapshot(program);
                snapshot.workouts().stream()
                    .flatMap(w -> w.content().blocks().stream())
                    .flatMap(b -> b.exercises().stream())
                    .forEach(e -> exerciseIds.add(e.exerciseId()));
                snapshots.add(snapshot);
              });
    }
    List<ExerciseView> exerciseViews =
        exercises.findAll(exerciseIds).stream().map(ManageExercisesUseCase::view).toList();
    return new SyncPage(cursor, snapshots, exerciseViews);
  }

  private ProgramSnapshot snapshot(Program program) {
    if (program.archived()) {
      return new ProgramSnapshot(
          program.id(),
          program.linkId(),
          program.name(),
          program.goal(),
          program.startsOn(),
          program.endsOn(),
          program.mode().code(),
          program.weeklyTarget(),
          true,
          List.of());
    }
    var items = new ArrayList<WorkoutSnapshot>();
    var position = 0;
    for (var slot : program.slots()) {
      var workout = workouts.find(slot.workoutId()).filter(w -> !w.archived());
      if (workout.isEmpty()) {
        continue;
      }
      position++;
      items.add(workoutSnapshot(workout.get(), position, slot.weekdays()));
    }
    return new ProgramSnapshot(
        program.id(),
        program.linkId(),
        program.name(),
        program.goal(),
        program.startsOn(),
        program.endsOn(),
        program.mode().code(),
        program.weeklyTarget(),
        false,
        items);
  }

  private static WorkoutSnapshot workoutSnapshot(Workout w, int position, Set<Integer> weekdays) {
    // nome/registro do exercício vão na lista de exercícios do sync, não repetidos em cada treino
    var view = WorkoutMapping.view(w, Map.of());
    return new WorkoutSnapshot(
        w.id(), w.name(), position, weekdays, w.versionId(), w.versionNumber(), view.content());
  }
}
