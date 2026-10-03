package br.com.moveup.training.application.usecase;

import br.com.moveup.training.application.port.in.ManageWorkouts.BlockInput;
import br.com.moveup.training.application.port.in.ManageWorkouts.BlockView;
import br.com.moveup.training.application.port.in.ManageWorkouts.ContentInput;
import br.com.moveup.training.application.port.in.ManageWorkouts.ContentView;
import br.com.moveup.training.application.port.in.ManageWorkouts.ExerciseInput;
import br.com.moveup.training.application.port.in.ManageWorkouts.ExerciseView;
import br.com.moveup.training.application.port.in.ManageWorkouts.SetInput;
import br.com.moveup.training.application.port.in.ManageWorkouts.WorkoutView;
import br.com.moveup.training.application.port.out.ExerciseCatalog.ExerciseRef;
import br.com.moveup.training.domain.exception.InvalidTrainingData;
import br.com.moveup.training.domain.model.PrescribedExercise;
import br.com.moveup.training.domain.model.PrescribedSet;
import br.com.moveup.training.domain.model.Workout;
import br.com.moveup.training.domain.model.WorkoutBlock;
import br.com.moveup.training.domain.model.WorkoutContent;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Entrada da API → domínio, e domínio → saída com nome e registro dos exercícios. */
final class WorkoutMapping {

  private WorkoutMapping() {}

  static WorkoutContent content(ContentInput input) {
    if (input == null) {
      return WorkoutContent.empty();
    }
    return new WorkoutContent(
        input.goal(),
        input.estimatedMinutes(),
        input.notes(),
        input.blocks() == null
            ? List.of()
            : input.blocks().stream().map(WorkoutMapping::block).toList());
  }

  private static WorkoutBlock block(BlockInput b) {
    return new WorkoutBlock(
        b.name(),
        WorkoutBlock.Method.fromCode(b.method()),
        b.preset(),
        b.rounds(),
        b.workSeconds(),
        b.restSeconds(),
        b.restBetweenRounds(),
        b.durationSeconds(),
        b.notes(),
        b.exercises() == null
            ? List.of()
            : b.exercises().stream().map(WorkoutMapping::exercise).toList());
  }

  private static PrescribedExercise exercise(ExerciseInput e) {
    return new PrescribedExercise(
        e.exerciseId(),
        e.restSeconds(),
        e.notes(),
        e.sets() == null ? List.of() : e.sets().stream().map(WorkoutMapping::set).toList());
  }

  private static PrescribedSet set(SetInput s) {
    return new PrescribedSet(
        s.type() == null ? PrescribedSet.SetType.NORMAL : PrescribedSet.SetType.fromCode(s.type()),
        s.repsMin(),
        s.repsMax(),
        s.loadKg(),
        s.durationSeconds(),
        s.distanceM(),
        s.targetRpe(),
        s.targetRir(),
        s.restSeconds());
  }

  /** Todo exercício do treino precisa ser da base ou da organização. */
  static void requireKnown(WorkoutContent content, Map<UUID, ExerciseRef> refs) {
    if (!refs.keySet().containsAll(content.exerciseIds())) {
      throw new InvalidTrainingData("exercise-unknown", "Exercício não encontrado na biblioteca.");
    }
  }

  static WorkoutView view(Workout w, Map<UUID, ExerciseRef> refs) {
    var c = w.content();
    return new WorkoutView(
        w.id(),
        w.name(),
        w.isTemplate(),
        w.programId(),
        w.sourceTemplateId(),
        w.revision(),
        w.versionId(),
        w.versionNumber(),
        new ContentView(
            c.goal(),
            c.estimatedMinutes(),
            c.notes(),
            c.blocks().stream().map(b -> blockView(b, refs)).toList()));
  }

  private static BlockView blockView(WorkoutBlock b, Map<UUID, ExerciseRef> refs) {
    return new BlockView(
        b.name(),
        b.method().code(),
        b.preset(),
        b.rounds(),
        b.workSeconds(),
        b.restSeconds(),
        b.restBetweenRounds(),
        b.durationSeconds(),
        b.notes(),
        b.exercises().stream()
            .map(
                e -> {
                  var ref = refs.get(e.exerciseId());
                  return new ExerciseView(
                      e.exerciseId(),
                      ref == null ? null : ref.name(),
                      ref == null ? null : ref.trackingType(),
                      ref == null ? null : ref.primaryMuscle(),
                      e.restSeconds(),
                      e.notes(),
                      e.sets().stream().map(WorkoutMapping::setView).toList());
                })
            .toList());
  }

  private static SetInput setView(PrescribedSet s) {
    return new SetInput(
        s.type().code(),
        s.repsMin(),
        s.repsMax(),
        s.loadKg(),
        s.durationSeconds(),
        s.distanceM(),
        s.targetRpe(),
        s.targetRir(),
        s.restSeconds());
  }
}
