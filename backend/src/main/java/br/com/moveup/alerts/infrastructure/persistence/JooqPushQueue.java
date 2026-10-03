package br.com.moveup.alerts.infrastructure.persistence;

import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.PUSH_MESSAGE;

import br.com.moveup.alerts.application.port.out.PushQueue;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** Fila de push (só o worker, como app_worker). */
@Repository
public class JooqPushQueue implements PushQueue {

  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final TypeReference<LinkedHashMap<String, String>> DATA = new TypeReference<>() {};

  private final DSLContext dsl;

  public JooqPushQueue(DSLContext dsl) {
    this.dsl = dsl;
  }

  @Override
  public void enqueue(
      UUID userId, String dedupeKey, String title, String body, Map<String, String> data) {
    dsl.insertInto(PUSH_MESSAGE)
        .set(PUSH_MESSAGE.USER_ID, userId)
        .set(PUSH_MESSAGE.DEDUPE_KEY, dedupeKey)
        .set(PUSH_MESSAGE.TITLE, title)
        .set(PUSH_MESSAGE.BODY, body)
        .set(PUSH_MESSAGE.DATA, JSONB.valueOf(JSON.writeValueAsString(data)))
        .onConflict(PUSH_MESSAGE.DEDUPE_KEY)
        .doNothing()
        .execute();
  }

  @Override
  public List<PendingPush> claim(int max, Instant now) {
    var at = now.atOffset(ZoneOffset.UTC);
    var ids =
        dsl.select(PUSH_MESSAGE.ID)
            .from(PUSH_MESSAGE)
            .where(PUSH_MESSAGE.STATUS.eq("pending"))
            .and(PUSH_MESSAGE.NEXT_ATTEMPT_AT.le(at))
            .orderBy(PUSH_MESSAGE.NEXT_ATTEMPT_AT)
            .limit(max)
            .forUpdate()
            .skipLocked()
            .fetch(PUSH_MESSAGE.ID);
    if (ids.isEmpty()) {
      return List.of();
    }
    return dsl.update(PUSH_MESSAGE)
        .set(PUSH_MESSAGE.STATUS, "sending")
        .set(PUSH_MESSAGE.SENDING_SINCE, at)
        .where(PUSH_MESSAGE.ID.in(ids))
        .returning(
            PUSH_MESSAGE.ID,
            PUSH_MESSAGE.USER_ID,
            PUSH_MESSAGE.TITLE,
            PUSH_MESSAGE.BODY,
            PUSH_MESSAGE.DATA,
            PUSH_MESSAGE.ATTEMPTS)
        .fetch(
            r ->
                new PendingPush(
                    r.getId(),
                    r.getUserId(),
                    r.getTitle(),
                    r.getBody(),
                    JSON.readValue(r.getData().data(), DATA),
                    r.getAttempts()));
  }

  @Override
  public void markSent(UUID pushId, Instant now) {
    dsl.update(PUSH_MESSAGE)
        .set(PUSH_MESSAGE.STATUS, "sent")
        .set(PUSH_MESSAGE.SENDING_SINCE, (OffsetDateTime) null)
        .set(PUSH_MESSAGE.SENT_AT, now.atOffset(ZoneOffset.UTC))
        .set(PUSH_MESSAGE.ATTEMPTS, PUSH_MESSAGE.ATTEMPTS.plus(1))
        .set(PUSH_MESSAGE.LAST_ERROR, (String) null)
        .where(PUSH_MESSAGE.ID.eq(pushId))
        .execute();
  }

  @Override
  public void markRetry(UUID pushId, int attempts, Instant nextAttemptAt, String error) {
    dsl.update(PUSH_MESSAGE)
        .set(PUSH_MESSAGE.STATUS, "pending")
        .set(PUSH_MESSAGE.SENDING_SINCE, (OffsetDateTime) null)
        .set(PUSH_MESSAGE.ATTEMPTS, attempts)
        .set(PUSH_MESSAGE.NEXT_ATTEMPT_AT, nextAttemptAt.atOffset(ZoneOffset.UTC))
        .set(PUSH_MESSAGE.LAST_ERROR, error)
        .where(PUSH_MESSAGE.ID.eq(pushId))
        .execute();
  }

  @Override
  public void markFailed(UUID pushId, String error) {
    dsl.update(PUSH_MESSAGE)
        .set(PUSH_MESSAGE.STATUS, "failed")
        .set(PUSH_MESSAGE.SENDING_SINCE, (OffsetDateTime) null)
        .set(PUSH_MESSAGE.ATTEMPTS, PUSH_MESSAGE.ATTEMPTS.plus(1))
        .set(PUSH_MESSAGE.LAST_ERROR, error)
        .where(PUSH_MESSAGE.ID.eq(pushId))
        .execute();
  }

  @Override
  public int failStale(Instant before) {
    return dsl.update(PUSH_MESSAGE)
        .set(PUSH_MESSAGE.STATUS, "failed")
        .set(PUSH_MESSAGE.SENDING_SINCE, (OffsetDateTime) null)
        .set(PUSH_MESSAGE.LAST_ERROR, "interrupted")
        .where(PUSH_MESSAGE.STATUS.eq("sending"))
        .and(PUSH_MESSAGE.SENDING_SINCE.lt(before.atOffset(ZoneOffset.UTC)))
        .execute();
  }

  /** Limpeza diária: enviados ou falhos há mais que {@code keepUntil}. */
  public int deleteFinishedBefore(Instant keepUntil) {
    return dsl.deleteFrom(PUSH_MESSAGE)
        .where(PUSH_MESSAGE.STATUS.in("sent", "failed"))
        .and(PUSH_MESSAGE.CREATED_AT.lt(keepUntil.atOffset(ZoneOffset.UTC)))
        .execute();
  }
}
