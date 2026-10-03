package br.com.moveup.execution.infrastructure.persistence;

import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.PERFORMED_EXERCISE;
import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.PERFORMED_SET;
import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.PERSONAL_RECORD;
import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.WORKOUT_SESSION;

import br.com.moveup.execution.application.port.out.RecordHistory;
import br.com.moveup.execution.domain.model.PersonalRecords;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

/** Séries e recordes (o worker conecta como app_worker; o RLS libera tudo para ele). */
@Repository
public class JooqRecordHistory implements RecordHistory {

  private static final List<String> FINISHED = List.of("completed", "partial");

  private final DSLContext dsl;

  public JooqRecordHistory(DSLContext dsl) {
    this.dsl = dsl;
  }

  @Override
  public Optional<Scope> scopeOf(UUID sessionId) {
    var clientId =
        dsl.select(WORKOUT_SESSION.CLIENT_ID)
            .from(WORKOUT_SESSION)
            .where(WORKOUT_SESSION.ID.eq(sessionId))
            .fetchOptional(WORKOUT_SESSION.CLIENT_ID);
    return clientId.map(
        client -> {
          var exercises = new HashSet<UUID>();
          exercises.addAll(
              dsl.selectDistinct(PERFORMED_EXERCISE.EXERCISE_ID)
                  .from(PERFORMED_EXERCISE)
                  .where(PERFORMED_EXERCISE.SESSION_ID.eq(sessionId))
                  .fetch(PERFORMED_EXERCISE.EXERCISE_ID));
          // exercício trocado na correção: o recorde antigo desta sessão também é refeito
          exercises.addAll(
              dsl.selectDistinct(PERSONAL_RECORD.EXERCISE_ID)
                  .from(PERSONAL_RECORD)
                  .where(PERSONAL_RECORD.SESSION_ID.eq(sessionId))
                  .fetch(PERSONAL_RECORD.EXERCISE_ID));
          return new Scope(client, exercises);
        });
  }

  @Override
  public List<PersonalRecords.SetPerformance> finishedSets(
      UUID clientId, Collection<UUID> exerciseIds) {
    var achieved =
        DSL.coalesce(
            PERFORMED_SET.COMPLETED_AT, WORKOUT_SESSION.FINISHED_AT, WORKOUT_SESSION.STARTED_AT);
    return dsl.select(
            WORKOUT_SESSION.ID,
            PERFORMED_SET.ID,
            PERFORMED_EXERCISE.EXERCISE_ID,
            achieved,
            PERFORMED_SET.REPS,
            PERFORMED_SET.LOAD_KG,
            PERFORMED_SET.DURATION_SECONDS,
            PERFORMED_SET.DISTANCE_M)
        .from(PERFORMED_SET)
        .join(PERFORMED_EXERCISE)
        .on(PERFORMED_EXERCISE.ID.eq(PERFORMED_SET.PERFORMED_EXERCISE_ID))
        .join(WORKOUT_SESSION)
        .on(WORKOUT_SESSION.ID.eq(PERFORMED_EXERCISE.SESSION_ID))
        .where(WORKOUT_SESSION.CLIENT_ID.eq(clientId))
        .and(WORKOUT_SESSION.STATUS.in(FINISHED))
        .and(PERFORMED_EXERCISE.EXERCISE_ID.in(exerciseIds))
        .and(PERFORMED_EXERCISE.STATUS.ne("skipped"))
        .and(PERFORMED_SET.COMPLETED.isTrue())
        .and(PERFORMED_SET.SET_TYPE.ne("warmup"))
        .fetch(
            r ->
                new PersonalRecords.SetPerformance(
                    r.value1(),
                    r.value2(),
                    r.value3(),
                    r.value4().toInstant(),
                    r.value5() == null ? null : r.value5().intValue(),
                    r.value6(),
                    r.value7(),
                    r.value8()));
  }

  @Override
  public void replace(
      UUID clientId, Collection<UUID> exerciseIds, List<PersonalRecords.Record> records) {
    dsl.deleteFrom(PERSONAL_RECORD)
        .where(PERSONAL_RECORD.CLIENT_ID.eq(clientId))
        .and(PERSONAL_RECORD.EXERCISE_ID.in(exerciseIds))
        .execute();
    for (var r : records) {
      dsl.insertInto(PERSONAL_RECORD)
          .set(PERSONAL_RECORD.CLIENT_ID, clientId)
          .set(PERSONAL_RECORD.EXERCISE_ID, r.exerciseId())
          .set(PERSONAL_RECORD.RECORD_TYPE, r.type().code())
          .set(PERSONAL_RECORD.VALUE, r.value())
          .set(PERSONAL_RECORD.LOAD_KG, r.loadKg())
          .set(PERSONAL_RECORD.REPS, r.reps() == null ? null : r.reps().shortValue())
          .set(PERSONAL_RECORD.SESSION_ID, r.sessionId())
          .set(PERSONAL_RECORD.PERFORMED_SET_ID, r.setId())
          .set(PERSONAL_RECORD.IS_CURRENT, r.current())
          .set(PERSONAL_RECORD.ACHIEVED_AT, r.achievedAt().atOffset(ZoneOffset.UTC))
          .execute();
    }
  }
}
