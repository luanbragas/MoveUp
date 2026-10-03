package br.com.moveup.training.infrastructure.persistence;

import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.PROGRAM;
import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.PROGRAM_SCHEDULE;
import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.WORKOUT;

import br.com.moveup.training.api.TrainingSchedule;
import br.com.moveup.training.domain.model.PlannedCount;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

/** Agenda dos programas ativos (worker, como app_worker). */
@Repository
public class JooqTrainingSchedule implements TrainingSchedule {

  private final DSLContext dsl;

  public JooqTrainingSchedule(DSLContext dsl) {
    this.dsl = dsl;
  }

  @Override
  public Map<UUID, Integer> plannedSessions(
      Collection<UUID> linkIds, LocalDate from, LocalDate to) {
    if (linkIds.isEmpty()) {
      return Map.of();
    }
    var programs =
        dsl.select(
                PROGRAM.ID,
                PROGRAM.COACHING_LINK_ID,
                PROGRAM.STARTS_ON,
                PROGRAM.ENDS_ON,
                PROGRAM.SCHEDULE_MODE,
                PROGRAM.WEEKLY_TARGET)
            .from(PROGRAM)
            .where(PROGRAM.COACHING_LINK_ID.in(linkIds))
            .and(PROGRAM.STATUS.eq("active"))
            .and(PROGRAM.DELETED_AT.isNull())
            .fetch();
    var weekdays = new HashMap<UUID, ArrayList<Integer>>();
    dsl.select(PROGRAM_SCHEDULE.PROGRAM_ID, PROGRAM_SCHEDULE.WEEKDAY)
        .from(PROGRAM_SCHEDULE)
        .join(WORKOUT)
        .on(WORKOUT.ID.eq(PROGRAM_SCHEDULE.WORKOUT_ID))
        .where(PROGRAM_SCHEDULE.PROGRAM_ID.in(programs.map(r -> r.value1())))
        .and(WORKOUT.DELETED_AT.isNull())
        .forEach(
            r ->
                weekdays
                    .computeIfAbsent(r.value1(), k -> new ArrayList<>())
                    .add(r.value2().intValue()));
    var planned = new HashMap<UUID, Integer>();
    for (var p : programs) {
      var sequence = "sequence".equals(p.value5());
      planned.merge(
          p.value2(),
          PlannedCount.between(
              from,
              to,
              p.value3(),
              p.value4(),
              sequence ? List.of() : weekdays.getOrDefault(p.value1(), new ArrayList<>()),
              sequence && p.value6() != null ? Integer.valueOf(p.value6()) : null),
          Integer::sum);
    }
    return planned;
  }
}
