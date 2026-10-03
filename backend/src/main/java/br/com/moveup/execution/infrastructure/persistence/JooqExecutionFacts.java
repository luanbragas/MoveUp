package br.com.moveup.execution.infrastructure.persistence;

import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.PAIN_REPORT;
import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.SESSION_FEEDBACK;
import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.WORKOUT_SESSION;

import br.com.moveup.execution.api.ExecutionActivity;
import br.com.moveup.execution.api.SessionFacts;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

/** Leituras do realizado para o worker (como app_worker). */
@Repository
public class JooqExecutionFacts implements SessionFacts, ExecutionActivity {

  private static final List<String> FINISHED = List.of("completed", "partial");

  private final DSLContext dsl;

  public JooqExecutionFacts(DSLContext dsl) {
    this.dsl = dsl;
  }

  @Override
  public Optional<SessionSummary> summary(UUID sessionId) {
    var pains =
        DSL.selectCount().from(PAIN_REPORT).where(PAIN_REPORT.SESSION_ID.eq(WORKOUT_SESSION.ID));
    return dsl.select(
            WORKOUT_SESSION.ID,
            WORKOUT_SESSION.CLIENT_ID,
            WORKOUT_SESSION.COACHING_LINK_ID,
            SESSION_FEEDBACK.EFFORT,
            SESSION_FEEDBACK.COMMENT.isNotNull(),
            DSL.field(pains),
            WORKOUT_SESSION.FINISHED_AT)
        .from(WORKOUT_SESSION)
        .leftJoin(SESSION_FEEDBACK)
        .on(SESSION_FEEDBACK.SESSION_ID.eq(WORKOUT_SESSION.ID))
        .where(WORKOUT_SESSION.ID.eq(sessionId))
        .fetchOptional(
            r ->
                new SessionSummary(
                    r.value1(),
                    r.value2(),
                    r.value3(),
                    r.value4() == null ? null : r.value4().intValue(),
                    Boolean.TRUE.equals(r.value5()),
                    r.value6() == null ? 0 : r.value6(),
                    r.value7() == null ? null : r.value7().toInstant()));
  }

  @Override
  public Map<UUID, Instant> lastFinishedAt(Collection<UUID> linkIds) {
    if (linkIds.isEmpty()) {
      return Map.of();
    }
    var last = DSL.max(WORKOUT_SESSION.FINISHED_AT);
    return dsl.select(WORKOUT_SESSION.COACHING_LINK_ID, last)
        .from(WORKOUT_SESSION)
        .where(WORKOUT_SESSION.COACHING_LINK_ID.in(linkIds))
        .and(WORKOUT_SESSION.STATUS.in(FINISHED))
        .groupBy(WORKOUT_SESSION.COACHING_LINK_ID)
        .fetchMap(r -> r.value1(), r -> r.value2().toInstant());
  }

  @Override
  public Map<UUID, Integer> finishedCount(Collection<UUID> linkIds, Instant from, Instant to) {
    if (linkIds.isEmpty()) {
      return Map.of();
    }
    return dsl.select(WORKOUT_SESSION.COACHING_LINK_ID, DSL.count())
        .from(WORKOUT_SESSION)
        .where(WORKOUT_SESSION.COACHING_LINK_ID.in(linkIds))
        .and(WORKOUT_SESSION.STATUS.in(FINISHED))
        .and(WORKOUT_SESSION.FINISHED_AT.ge(from.atOffset(ZoneOffset.UTC)))
        .and(WORKOUT_SESSION.FINISHED_AT.lt(to.atOffset(ZoneOffset.UTC)))
        .groupBy(WORKOUT_SESSION.COACHING_LINK_ID)
        .fetchMap(r -> r.value1(), r -> r.value2());
  }
}
