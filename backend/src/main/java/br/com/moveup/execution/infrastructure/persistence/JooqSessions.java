package br.com.moveup.execution.infrastructure.persistence;

import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.OUTBOX_EVENT;
import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.PAIN_REPORT;
import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.PERFORMED_EXERCISE;
import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.PERFORMED_SET;
import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.SESSION_FEEDBACK;
import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.WORKOUT_SESSION;

import br.com.moveup.execution.application.port.out.Sessions;
import br.com.moveup.execution.domain.model.PerformedSession;
import br.com.moveup.execution.domain.model.SyncOutcome;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

/**
 * Sessões realizadas, com RLS (dado do aluno). A sessão é gravada inteira: na edição mais recente
 * os filhos (exercícios, séries, feedback, dores) são substituídos pelos do aparelho.
 */
@Repository
public class JooqSessions implements Sessions {

  private final DSLContext dsl;

  public JooqSessions(DSLContext dsl) {
    this.dsl = dsl;
  }

  @Override
  public Optional<SyncOutcome.Stored> lockStored(UUID sessionId) {
    return dsl.select(WORKOUT_SESSION.STATUS, WORKOUT_SESSION.CLIENT_UPDATED_AT)
        .from(WORKOUT_SESSION)
        .where(WORKOUT_SESSION.ID.eq(sessionId))
        .forUpdate()
        .fetchOptional(
            r ->
                new SyncOutcome.Stored(
                    PerformedSession.Status.fromCode(r.value1()), r.value2().toInstant()));
  }

  @Override
  public void write(
      PerformedSession s, UUID clientId, UUID performedByUser, boolean editedAfterFinish) {
    var edited =
        editedAfterFinish
            ? DSL.currentOffsetDateTime()
            : DSL.val((OffsetDateTime) null, WORKOUT_SESSION.EDITED_AFTER_FINISH_AT);
    dsl.insertInto(WORKOUT_SESSION)
        .set(WORKOUT_SESSION.ID, s.id())
        .set(WORKOUT_SESSION.CLIENT_ID, clientId)
        .set(WORKOUT_SESSION.COACHING_LINK_ID, s.linkId())
        .set(WORKOUT_SESSION.PROGRAM_ID, s.programId())
        .set(WORKOUT_SESSION.WORKOUT_ID, s.workoutId())
        .set(WORKOUT_SESSION.WORKOUT_VERSION_ID, s.workoutVersionId())
        .set(WORKOUT_SESSION.STATUS, s.status().code())
        .set(WORKOUT_SESSION.STARTED_AT, utc(s.startedAt()))
        .set(WORKOUT_SESSION.FINISHED_AT, utc(s.finishedAt()))
        .set(WORKOUT_SESSION.DURATION_SECONDS, s.durationSeconds())
        .set(WORKOUT_SESSION.COMPLETION_RATIO, s.completionRatio())
        .set(WORKOUT_SESSION.PERFORMED_BY, s.performedBy().code())
        .set(WORKOUT_SESSION.PERFORMED_BY_USER, performedByUser)
        .set(WORKOUT_SESSION.CLIENT_UPDATED_AT, utc(s.clientUpdatedAt()))
        .onConflict(WORKOUT_SESSION.ID)
        .doUpdate()
        .set(WORKOUT_SESSION.STATUS, s.status().code())
        .set(WORKOUT_SESSION.FINISHED_AT, utc(s.finishedAt()))
        .set(WORKOUT_SESSION.DURATION_SECONDS, s.durationSeconds())
        .set(WORKOUT_SESSION.COMPLETION_RATIO, s.completionRatio())
        .set(WORKOUT_SESSION.CLIENT_UPDATED_AT, utc(s.clientUpdatedAt()))
        .set(
            WORKOUT_SESSION.EDITED_AFTER_FINISH_AT,
            DSL.coalesce(edited, WORKOUT_SESSION.EDITED_AFTER_FINISH_AT))
        .execute();

    // filhos: o aparelho manda o estado inteiro
    dsl.deleteFrom(PERFORMED_EXERCISE).where(PERFORMED_EXERCISE.SESSION_ID.eq(s.id())).execute();
    dsl.deleteFrom(SESSION_FEEDBACK).where(SESSION_FEEDBACK.SESSION_ID.eq(s.id())).execute();
    for (var e : s.exercises()) {
      dsl.insertInto(PERFORMED_EXERCISE)
          .set(PERFORMED_EXERCISE.ID, e.id())
          .set(PERFORMED_EXERCISE.SESSION_ID, s.id())
          .set(PERFORMED_EXERCISE.CLIENT_ID, clientId)
          .set(PERFORMED_EXERCISE.EXERCISE_ID, e.exerciseId())
          .set(PERFORMED_EXERCISE.POSITION, (short) e.position())
          .set(PERFORMED_EXERCISE.STATUS, e.status())
          .set(PERFORMED_EXERCISE.SUBSTITUTED_FROM, e.substitutedFrom())
          .set(PERFORMED_EXERCISE.NOTES, e.notes())
          .set(PERFORMED_EXERCISE.CLIENT_UPDATED_AT, utc(s.clientUpdatedAt()))
          .execute();
      for (var set : e.sets()) {
        dsl.insertInto(PERFORMED_SET)
            .set(PERFORMED_SET.ID, set.id())
            .set(PERFORMED_SET.PERFORMED_EXERCISE_ID, e.id())
            .set(PERFORMED_SET.CLIENT_ID, clientId)
            .set(PERFORMED_SET.SET_NUMBER, (short) set.setNumber())
            .set(PERFORMED_SET.SET_TYPE, set.setType())
            .set(PERFORMED_SET.SIDE, set.side())
            .set(PERFORMED_SET.REPS, set.reps() == null ? null : set.reps().shortValue())
            .set(PERFORMED_SET.LOAD_KG, set.loadKg())
            .set(PERFORMED_SET.DURATION_SECONDS, set.durationSeconds())
            .set(PERFORMED_SET.DISTANCE_M, set.distanceM())
            .set(PERFORMED_SET.RPE, set.rpe())
            .set(PERFORMED_SET.RIR, set.rir() == null ? null : set.rir().shortValue())
            .set(PERFORMED_SET.COMPLETED, set.completed())
            .set(PERFORMED_SET.COMPLETED_AT, utc(set.completedAt()))
            .set(PERFORMED_SET.CLIENT_UPDATED_AT, utc(s.clientUpdatedAt()))
            .execute();
      }
    }
    var feedback = s.feedback();
    if (feedback != null) {
      dsl.insertInto(SESSION_FEEDBACK)
          .set(SESSION_FEEDBACK.SESSION_ID, s.id())
          .set(SESSION_FEEDBACK.CLIENT_ID, clientId)
          .set(SESSION_FEEDBACK.EFFORT, (short) feedback.effort())
          .set(SESSION_FEEDBACK.COMMENT, feedback.comment())
          .set(SESSION_FEEDBACK.HAS_PAIN, feedback.hasPain())
          .execute();
      for (var pain : feedback.pains()) {
        dsl.insertInto(PAIN_REPORT)
            .set(PAIN_REPORT.ID, pain.id())
            .set(PAIN_REPORT.SESSION_ID, s.id())
            .set(PAIN_REPORT.CLIENT_ID, clientId)
            .set(PAIN_REPORT.BODY_REGION, pain.bodyRegion())
            .set(PAIN_REPORT.EXERCISE_ID, pain.exerciseId())
            .set(
                PAIN_REPORT.INTENSITY,
                pain.intensity() == null ? null : pain.intensity().shortValue())
            .set(PAIN_REPORT.DESCRIPTION, pain.description())
            .execute();
      }
    }
  }

  @Override
  public void announceFinished(UUID sessionId, UUID clientId) {
    // payload só com ids: dado de saúde nunca vai para evento (CLAUDE.md, regra 7)
    dsl.insertInto(OUTBOX_EVENT)
        .set(OUTBOX_EVENT.AGGREGATE_TYPE, "workout_session")
        .set(OUTBOX_EVENT.AGGREGATE_ID, sessionId)
        .set(OUTBOX_EVENT.TYPE, "session.finished")
        .set(
            OUTBOX_EVENT.PAYLOAD,
            JSONB.valueOf(
                "{\"sessionId\": \"" + sessionId + "\", \"clientId\": \"" + clientId + "\"}"))
        .execute();
  }

  private static OffsetDateTime utc(Instant instant) {
    return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
  }
}
