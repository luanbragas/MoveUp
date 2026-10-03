package br.com.moveup.training.infrastructure.persistence;

import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.PRESCRIBED_EXERCISE;
import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.PROGRAM;
import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.PROGRAM_SCHEDULE;
import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.WORKOUT;
import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.WORKOUT_BLOCK;
import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.WORKOUT_VERSION;

import br.com.moveup.training.application.port.out.Programs;
import br.com.moveup.training.domain.model.Program;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

/**
 * Programas têm RLS (client_id): o personal só lê e grava os dos alunos com vínculo pendente ou
 * ativo. Treinos do programa ficam em workout (sem RLS) com program_id.
 */
@Repository
public class JooqPrograms implements Programs {

  private final DSLContext dsl;

  public JooqPrograms(DSLContext dsl) {
    this.dsl = dsl;
  }

  @Override
  public Optional<Program> find(UUID programId) {
    return dsl.selectFrom(PROGRAM)
        .where(PROGRAM.ID.eq(programId))
        .fetchOptional()
        .map(
            p -> {
              var weekdays = new HashMap<UUID, Set<Integer>>();
              dsl.select(PROGRAM_SCHEDULE.WORKOUT_ID, PROGRAM_SCHEDULE.WEEKDAY)
                  .from(PROGRAM_SCHEDULE)
                  .where(PROGRAM_SCHEDULE.PROGRAM_ID.eq(programId))
                  .forEach(
                      r ->
                          weekdays
                              .computeIfAbsent(r.value1(), k -> new HashSet<>())
                              .add(r.value2().intValue()));
              var slots =
                  dsl
                      .select(WORKOUT.ID)
                      .from(WORKOUT)
                      .where(WORKOUT.PROGRAM_ID.eq(programId))
                      .and(WORKOUT.STATUS.ne("archived"))
                      .and(WORKOUT.DELETED_AT.isNull())
                      .orderBy(WORKOUT.SEQUENCE_POSITION.asc().nullsLast(), WORKOUT.CREATED_AT)
                      .fetch(WORKOUT.ID)
                      .stream()
                      .map(id -> new Program.Slot(id, weekdays.getOrDefault(id, Set.of())))
                      .toList();
              return new Program(
                  p.getId(),
                  p.getCoachingLinkId(),
                  p.getClientId(),
                  p.getName(),
                  p.getGoal(),
                  p.getStartsOn(),
                  p.getEndsOn(),
                  Program.ScheduleMode.fromCode(p.getScheduleMode()),
                  p.getWeeklyTarget() == null ? null : p.getWeeklyTarget().intValue(),
                  slots,
                  p.getRevision(),
                  "archived".equals(p.getStatus()) || p.getDeletedAt() != null);
            });
  }

  @Override
  public Optional<UUID> activeFor(UUID linkId) {
    return dsl.select(PROGRAM.ID)
        .from(PROGRAM)
        .where(PROGRAM.COACHING_LINK_ID.eq(linkId))
        .and(PROGRAM.STATUS.eq("active"))
        .and(PROGRAM.DELETED_AT.isNull())
        .orderBy(PROGRAM.CREATED_AT.desc())
        .limit(1)
        .fetchOptional(PROGRAM.ID);
  }

  @Override
  public List<UUID> changedForOwnClient(Instant since) {
    var own = DSL.field("own_client_id()", UUID.class);
    var changedWorkout =
        DSL.exists(
            DSL.selectOne()
                .from(WORKOUT)
                .where(WORKOUT.PROGRAM_ID.eq(PROGRAM.ID))
                .and(WORKOUT.UPDATED_AT.gt(since.atOffset(ZoneOffset.UTC))));
    var condition = PROGRAM.CLIENT_ID.eq(own);
    if (since != Instant.EPOCH) {
      condition =
          condition.and(PROGRAM.UPDATED_AT.gt(since.atOffset(ZoneOffset.UTC)).or(changedWorkout));
    } else {
      // primeira sincronização: só o que vale (sem tombstones antigos)
      condition = condition.and(PROGRAM.STATUS.eq("active")).and(PROGRAM.DELETED_AT.isNull());
    }
    return dsl.select(PROGRAM.ID)
        .from(PROGRAM)
        .where(condition)
        .orderBy(PROGRAM.CREATED_AT)
        .fetch(PROGRAM.ID);
  }

  @Override
  public void insert(Program p) {
    dsl.insertInto(PROGRAM)
        .set(PROGRAM.ID, p.id())
        .set(PROGRAM.COACHING_LINK_ID, p.linkId())
        .set(PROGRAM.CLIENT_ID, p.clientId())
        .set(PROGRAM.NAME, p.name())
        .set(PROGRAM.GOAL, p.goal())
        .set(PROGRAM.STARTS_ON, p.startsOn())
        .set(PROGRAM.ENDS_ON, p.endsOn())
        .set(PROGRAM.SCHEDULE_MODE, p.mode().code())
        .set(PROGRAM.WEEKLY_TARGET, p.weeklyTarget() == null ? null : p.weeklyTarget().shortValue())
        .set(PROGRAM.REVISION, p.revision())
        .execute();
  }

  @Override
  public void save(Program p) {
    dsl.update(PROGRAM)
        .set(PROGRAM.NAME, p.name())
        .set(PROGRAM.GOAL, p.goal())
        .set(PROGRAM.STARTS_ON, p.startsOn())
        .set(PROGRAM.ENDS_ON, p.endsOn())
        .set(PROGRAM.SCHEDULE_MODE, p.mode().code())
        .set(PROGRAM.WEEKLY_TARGET, p.weeklyTarget() == null ? null : p.weeklyTarget().shortValue())
        .set(PROGRAM.REVISION, p.revision())
        .where(PROGRAM.ID.eq(p.id()))
        .execute();
    // a posição é única por programa e a restrição não é adiável: zera e regrava
    dsl.update(WORKOUT)
        .setNull(WORKOUT.SEQUENCE_POSITION)
        .where(WORKOUT.PROGRAM_ID.eq(p.id()))
        .execute();
    short position = 0;
    for (var slot : p.slots()) {
      position++;
      dsl.update(WORKOUT)
          .set(WORKOUT.SEQUENCE_POSITION, position)
          .where(WORKOUT.ID.eq(slot.workoutId()))
          .and(WORKOUT.PROGRAM_ID.eq(p.id()))
          .execute();
    }
    dsl.deleteFrom(PROGRAM_SCHEDULE).where(PROGRAM_SCHEDULE.PROGRAM_ID.eq(p.id())).execute();
    if (p.mode() == Program.ScheduleMode.FIXED_DAYS) {
      for (var slot : p.slots()) {
        for (var day : slot.weekdays()) {
          dsl.insertInto(PROGRAM_SCHEDULE)
              .set(PROGRAM_SCHEDULE.PROGRAM_ID, p.id())
              .set(PROGRAM_SCHEDULE.WORKOUT_ID, slot.workoutId())
              .set(PROGRAM_SCHEDULE.WEEKDAY, day.shortValue())
              .execute();
        }
      }
    }
  }

  @Override
  public void archiveActive(UUID linkId) {
    dsl.update(PROGRAM)
        .set(PROGRAM.STATUS, "archived")
        .set(PROGRAM.DELETED_AT, DSL.currentOffsetDateTime())
        .set(PROGRAM.REVISION, PROGRAM.REVISION.plus(1))
        .where(PROGRAM.COACHING_LINK_ID.eq(linkId))
        .and(PROGRAM.STATUS.eq("active"))
        .execute();
  }

  @Override
  public void saveArchive(Program p) {
    dsl.update(PROGRAM)
        .set(PROGRAM.STATUS, "archived")
        .set(PROGRAM.DELETED_AT, DSL.currentOffsetDateTime())
        .set(PROGRAM.REVISION, p.revision())
        .where(PROGRAM.ID.eq(p.id()))
        .execute();
  }

  @Override
  public Map<UUID, WorkoutStats> workoutStats(UUID programId) {
    var exercises =
        DSL.selectCount()
            .from(PRESCRIBED_EXERCISE)
            .join(WORKOUT_BLOCK)
            .on(WORKOUT_BLOCK.ID.eq(PRESCRIBED_EXERCISE.BLOCK_ID))
            .where(WORKOUT_BLOCK.WORKOUT_VERSION_ID.eq(WORKOUT.CURRENT_VERSION_ID))
            .asField("exercises");
    var stats = new LinkedHashMap<UUID, WorkoutStats>();
    dsl.select(WORKOUT.ID, WORKOUT.NAME, WORKOUT_VERSION.ESTIMATED_MINUTES, exercises)
        .from(WORKOUT)
        .join(WORKOUT_VERSION)
        .on(WORKOUT_VERSION.ID.eq(WORKOUT.CURRENT_VERSION_ID))
        .where(WORKOUT.PROGRAM_ID.eq(programId))
        .and(WORKOUT.STATUS.ne("archived"))
        .and(WORKOUT.DELETED_AT.isNull())
        .forEach(
            r ->
                stats.put(
                    r.get(WORKOUT.ID),
                    new WorkoutStats(
                        r.get(WORKOUT.NAME),
                        r.get(WORKOUT_VERSION.ESTIMATED_MINUTES) == null
                            ? null
                            : r.get(WORKOUT_VERSION.ESTIMATED_MINUTES).intValue(),
                        r.get("exercises", Integer.class))));
    return stats;
  }
}
