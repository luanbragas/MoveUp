package br.com.moveup.training.infrastructure.persistence;

import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.PRESCRIBED_EXERCISE;
import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.PRESCRIBED_SET;
import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.WORKOUT;
import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.WORKOUT_BLOCK;
import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.WORKOUT_VERSION;

import br.com.moveup.shared.infrastructure.persistence.jooq.tables.records.PrescribedSetRecord;
import br.com.moveup.training.application.port.in.ManageWorkouts.WorkoutSummary;
import br.com.moveup.training.application.port.out.Workouts;
import br.com.moveup.training.domain.model.PrescribedExercise;
import br.com.moveup.training.domain.model.PrescribedSet;
import br.com.moveup.training.domain.model.Workout;
import br.com.moveup.training.domain.model.WorkoutBlock;
import br.com.moveup.training.domain.model.WorkoutContent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

/**
 * Treinos sem RLS (não são dado de saúde): o caso de uso confere a organização. O conteúdo é
 * gravado por inteiro a cada "Salvar"; posições começam em 1.
 */
@Repository
public class JooqWorkouts implements Workouts {

  private final DSLContext dsl;

  public JooqWorkouts(DSLContext dsl) {
    this.dsl = dsl;
  }

  @Override
  public Optional<Workout> find(UUID workoutId) {
    var row =
        dsl.select(
                WORKOUT.ID,
                WORKOUT.ORGANIZATION_ID,
                WORKOUT.PROGRAM_ID,
                WORKOUT.SOURCE_TEMPLATE_ID,
                WORKOUT.NAME,
                WORKOUT.REVISION,
                WORKOUT.STATUS,
                WORKOUT_VERSION.ID,
                WORKOUT_VERSION.VERSION_NUMBER,
                WORKOUT_VERSION.GOAL,
                WORKOUT_VERSION.ESTIMATED_MINUTES,
                WORKOUT_VERSION.NOTES)
            .from(WORKOUT)
            .join(WORKOUT_VERSION)
            .on(WORKOUT_VERSION.ID.eq(WORKOUT.CURRENT_VERSION_ID))
            .where(WORKOUT.ID.eq(workoutId))
            .and(WORKOUT.DELETED_AT.isNull())
            .fetchOptional();
    return row.map(
        r ->
            new Workout(
                r.get(WORKOUT.ID),
                r.get(WORKOUT.ORGANIZATION_ID),
                r.get(WORKOUT.PROGRAM_ID),
                r.get(WORKOUT.SOURCE_TEMPLATE_ID),
                r.get(WORKOUT.NAME),
                r.get(WORKOUT.REVISION),
                r.get(WORKOUT_VERSION.ID),
                r.get(WORKOUT_VERSION.VERSION_NUMBER),
                new WorkoutContent(
                    r.get(WORKOUT_VERSION.GOAL),
                    toInt(r.get(WORKOUT_VERSION.ESTIMATED_MINUTES)),
                    r.get(WORKOUT_VERSION.NOTES),
                    blocks(r.get(WORKOUT_VERSION.ID))),
                "archived".equals(r.get(WORKOUT.STATUS))));
  }

  private List<WorkoutBlock> blocks(UUID versionId) {
    var blockRows =
        dsl.selectFrom(WORKOUT_BLOCK)
            .where(WORKOUT_BLOCK.WORKOUT_VERSION_ID.eq(versionId))
            .orderBy(WORKOUT_BLOCK.POSITION)
            .fetch();
    if (blockRows.isEmpty()) {
      return List.of();
    }
    var blockIds = blockRows.map(b -> b.getId());
    var exerciseRows =
        dsl.selectFrom(PRESCRIBED_EXERCISE)
            .where(PRESCRIBED_EXERCISE.BLOCK_ID.in(blockIds))
            .orderBy(PRESCRIBED_EXERCISE.BLOCK_ID, PRESCRIBED_EXERCISE.POSITION)
            .fetch();
    var setRows =
        exerciseRows.isEmpty()
            ? List.<PrescribedSetRecord>of()
            : dsl.selectFrom(PRESCRIBED_SET)
                .where(PRESCRIBED_SET.PRESCRIBED_EXERCISE_ID.in(exerciseRows.map(e -> e.getId())))
                .orderBy(PRESCRIBED_SET.PRESCRIBED_EXERCISE_ID, PRESCRIBED_SET.SET_NUMBER)
                .fetch();

    var blocks = new ArrayList<WorkoutBlock>();
    for (var b : blockRows) {
      var exercises = new ArrayList<PrescribedExercise>();
      for (var e : exerciseRows) {
        if (!e.getBlockId().equals(b.getId())) {
          continue;
        }
        var sets = new ArrayList<PrescribedSet>();
        for (var s : setRows) {
          if (s.getPrescribedExerciseId().equals(e.getId())) {
            sets.add(
                new PrescribedSet(
                    PrescribedSet.SetType.fromCode(s.getSetType()),
                    toInt(s.getRepsMin()),
                    toInt(s.getRepsMax()),
                    s.getLoadKg() == null ? null : s.getLoadKg().stripTrailingZeros(),
                    s.getDurationSeconds(),
                    s.getDistanceM(),
                    s.getTargetRpe(),
                    toInt(s.getTargetRir()),
                    s.getRestSeconds()));
          }
        }
        exercises.add(
            new PrescribedExercise(e.getExerciseId(), e.getRestSeconds(), e.getNotes(), sets));
      }
      blocks.add(
          new WorkoutBlock(
              b.getName(),
              WorkoutBlock.Method.fromCode(b.getMethod()),
              b.getPreset(),
              toInt(b.getRounds()),
              b.getWorkSeconds(),
              b.getRestSeconds(),
              b.getRestBetweenRounds(),
              b.getDurationSeconds(),
              b.getNotes(),
              exercises));
    }
    return blocks;
  }

  @Override
  public void insert(Workout w, UUID createdBy) {
    dsl.insertInto(WORKOUT)
        .set(WORKOUT.ID, w.id())
        .set(WORKOUT.ORGANIZATION_ID, w.organizationId())
        .set(WORKOUT.PROGRAM_ID, w.programId())
        .set(WORKOUT.IS_TEMPLATE, w.isTemplate())
        .set(WORKOUT.SOURCE_TEMPLATE_ID, w.sourceTemplateId())
        .set(WORKOUT.NAME, w.name())
        .set(WORKOUT.CURRENT_VERSION_ID, w.versionId())
        .set(WORKOUT.REVISION, w.revision())
        .execute();
    insertVersion(w, createdBy);
  }

  @Override
  public void saveEdit(Workout w, boolean newVersion, UUID editedBy) {
    if (newVersion) {
      insertVersion(w, editedBy);
    } else {
      dsl.update(WORKOUT_VERSION)
          .set(WORKOUT_VERSION.GOAL, w.content().goal())
          .set(WORKOUT_VERSION.ESTIMATED_MINUTES, toShort(w.content().estimatedMinutes()))
          .set(WORKOUT_VERSION.NOTES, w.content().notes())
          .where(WORKOUT_VERSION.ID.eq(w.versionId()))
          .execute();
      // blocos apagam exercícios e séries em cascata
      dsl.deleteFrom(WORKOUT_BLOCK)
          .where(WORKOUT_BLOCK.WORKOUT_VERSION_ID.eq(w.versionId()))
          .execute();
      insertContent(w.versionId(), w.content());
    }
    dsl.update(WORKOUT)
        .set(WORKOUT.NAME, w.name())
        .set(WORKOUT.REVISION, w.revision())
        .set(WORKOUT.CURRENT_VERSION_ID, w.versionId())
        .where(WORKOUT.ID.eq(w.id()))
        .execute();
  }

  @Override
  public void saveArchive(Workout w) {
    dsl.update(WORKOUT)
        .set(WORKOUT.STATUS, "archived")
        .set(WORKOUT.DELETED_AT, DSL.currentOffsetDateTime())
        .set(WORKOUT.REVISION, w.revision())
        .where(WORKOUT.ID.eq(w.id()))
        .execute();
  }

  @Override
  public List<WorkoutSummary> templates(UUID organizationId) {
    var blocks =
        DSL.selectCount()
            .from(WORKOUT_BLOCK)
            .where(WORKOUT_BLOCK.WORKOUT_VERSION_ID.eq(WORKOUT.CURRENT_VERSION_ID))
            .asField("blocks");
    var exercises =
        DSL.selectCount()
            .from(PRESCRIBED_EXERCISE)
            .join(WORKOUT_BLOCK)
            .on(WORKOUT_BLOCK.ID.eq(PRESCRIBED_EXERCISE.BLOCK_ID))
            .where(WORKOUT_BLOCK.WORKOUT_VERSION_ID.eq(WORKOUT.CURRENT_VERSION_ID))
            .asField("exercises");
    return dsl.select(
            WORKOUT.ID,
            WORKOUT.NAME,
            WORKOUT.UPDATED_AT,
            WORKOUT_VERSION.ESTIMATED_MINUTES,
            blocks,
            exercises)
        .from(WORKOUT)
        .join(WORKOUT_VERSION)
        .on(WORKOUT_VERSION.ID.eq(WORKOUT.CURRENT_VERSION_ID))
        .where(WORKOUT.ORGANIZATION_ID.eq(organizationId))
        .and(WORKOUT.IS_TEMPLATE.isTrue())
        .and(WORKOUT.STATUS.ne("archived"))
        .orderBy(WORKOUT.UPDATED_AT.desc())
        .fetch(
            r ->
                new WorkoutSummary(
                    r.get(WORKOUT.ID),
                    r.get(WORKOUT.NAME),
                    r.get("exercises", Integer.class),
                    r.get("blocks", Integer.class),
                    toInt(r.get(WORKOUT_VERSION.ESTIMATED_MINUTES)),
                    r.get(WORKOUT.UPDATED_AT).toInstant()));
  }

  private void insertVersion(Workout w, UUID createdBy) {
    dsl.insertInto(WORKOUT_VERSION)
        .set(WORKOUT_VERSION.ID, w.versionId())
        .set(WORKOUT_VERSION.WORKOUT_ID, w.id())
        .set(WORKOUT_VERSION.VERSION_NUMBER, w.versionNumber())
        .set(WORKOUT_VERSION.GOAL, w.content().goal())
        .set(WORKOUT_VERSION.ESTIMATED_MINUTES, toShort(w.content().estimatedMinutes()))
        .set(WORKOUT_VERSION.NOTES, w.content().notes())
        .set(WORKOUT_VERSION.CREATED_BY, createdBy)
        .execute();
    insertContent(w.versionId(), w.content());
  }

  private void insertContent(UUID versionId, WorkoutContent content) {
    short blockPosition = 0;
    for (var block : content.blocks()) {
      blockPosition++;
      var blockId =
          dsl.insertInto(WORKOUT_BLOCK)
              .set(WORKOUT_BLOCK.WORKOUT_VERSION_ID, versionId)
              .set(WORKOUT_BLOCK.POSITION, blockPosition)
              .set(WORKOUT_BLOCK.NAME, block.name())
              .set(WORKOUT_BLOCK.METHOD, block.method().code())
              .set(WORKOUT_BLOCK.PRESET, block.preset())
              .set(WORKOUT_BLOCK.ROUNDS, toShort(block.rounds()))
              .set(WORKOUT_BLOCK.WORK_SECONDS, block.workSeconds())
              .set(WORKOUT_BLOCK.REST_SECONDS, block.restSeconds())
              .set(WORKOUT_BLOCK.REST_BETWEEN_ROUNDS, block.restBetweenRounds())
              .set(WORKOUT_BLOCK.DURATION_SECONDS, block.durationSeconds())
              .set(WORKOUT_BLOCK.NOTES, block.notes())
              .returningResult(WORKOUT_BLOCK.ID)
              .fetchSingle(WORKOUT_BLOCK.ID);
      short exercisePosition = 0;
      for (var exercise : block.exercises()) {
        exercisePosition++;
        var exerciseId =
            dsl.insertInto(PRESCRIBED_EXERCISE)
                .set(PRESCRIBED_EXERCISE.BLOCK_ID, blockId)
                .set(PRESCRIBED_EXERCISE.EXERCISE_ID, exercise.exerciseId())
                .set(PRESCRIBED_EXERCISE.POSITION, exercisePosition)
                .set(PRESCRIBED_EXERCISE.REST_SECONDS, exercise.restSeconds())
                .set(PRESCRIBED_EXERCISE.NOTES, exercise.notes())
                .returningResult(PRESCRIBED_EXERCISE.ID)
                .fetchSingle(PRESCRIBED_EXERCISE.ID);
        if (exercise.sets().isEmpty()) {
          continue;
        }
        var insert =
            dsl.insertInto(
                PRESCRIBED_SET,
                PRESCRIBED_SET.PRESCRIBED_EXERCISE_ID,
                PRESCRIBED_SET.SET_NUMBER,
                PRESCRIBED_SET.SET_TYPE,
                PRESCRIBED_SET.REPS_MIN,
                PRESCRIBED_SET.REPS_MAX,
                PRESCRIBED_SET.LOAD_KG,
                PRESCRIBED_SET.DURATION_SECONDS,
                PRESCRIBED_SET.DISTANCE_M,
                PRESCRIBED_SET.TARGET_RPE,
                PRESCRIBED_SET.TARGET_RIR,
                PRESCRIBED_SET.REST_SECONDS);
        short number = 0;
        for (var set : exercise.sets()) {
          number++;
          insert =
              insert.values(
                  exerciseId,
                  number,
                  set.type().code(),
                  toShort(set.repsMin()),
                  toShort(set.repsMax()),
                  set.loadKg(),
                  set.durationSeconds(),
                  set.distanceM(),
                  set.targetRpe(),
                  toShort(set.targetRir()),
                  set.restSeconds());
        }
        insert.execute();
      }
    }
  }

  private static Integer toInt(Short value) {
    return value == null ? null : value.intValue();
  }

  private static Short toShort(Integer value) {
    return value == null ? null : value.shortValue();
  }
}
