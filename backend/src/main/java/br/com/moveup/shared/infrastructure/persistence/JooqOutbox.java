package br.com.moveup.shared.infrastructure.persistence;

import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.OUTBOX_EVENT;

import br.com.moveup.shared.application.outbox.OutboxEvent;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

/**
 * Fila do outbox (worker, como {@code app_worker}). Sempre dentro de transação: o evento fica
 * travado ({@code FOR UPDATE SKIP LOCKED}) até o commit, então duas instâncias nunca pegam o mesmo.
 */
@Repository
public class JooqOutbox {

  private final DSLContext dsl;

  public JooqOutbox(DSLContext dsl) {
    this.dsl = dsl;
  }

  /** O próximo evento vencido, travado nesta transação; vazio se a fila está em dia. */
  public Optional<OutboxEvent> lockNext() {
    return dsl.select(
            OUTBOX_EVENT.ID, OUTBOX_EVENT.TYPE, OUTBOX_EVENT.AGGREGATE_ID, OUTBOX_EVENT.ATTEMPTS)
        .from(OUTBOX_EVENT)
        .where(OUTBOX_EVENT.PROCESSED_AT.isNull())
        .and(OUTBOX_EVENT.NEXT_ATTEMPT_AT.le(DSL.currentOffsetDateTime()))
        .orderBy(OUTBOX_EVENT.ID)
        .limit(1)
        .forUpdate()
        .skipLocked()
        .fetchOptional(r -> new OutboxEvent(r.value1(), r.value2(), r.value3(), r.value4()));
  }

  public void markProcessed(long id) {
    dsl.update(OUTBOX_EVENT)
        .set(OUTBOX_EVENT.PROCESSED_AT, DSL.currentOffsetDateTime())
        .set(OUTBOX_EVENT.LAST_ERROR, (String) null)
        .where(OUTBOX_EVENT.ID.eq(id))
        .execute();
  }

  /** Falhou: conta a tentativa e agenda a próxima. O erro é só o tipo (nunca a mensagem). */
  public void markFailed(long id, int attempts, Instant nextAttemptAt, String error) {
    dsl.update(OUTBOX_EVENT)
        .set(OUTBOX_EVENT.ATTEMPTS, attempts)
        .set(OUTBOX_EVENT.NEXT_ATTEMPT_AT, nextAttemptAt.atOffset(ZoneOffset.UTC))
        .set(OUTBOX_EVENT.LAST_ERROR, error)
        .where(OUTBOX_EVENT.ID.eq(id))
        .execute();
  }

  /**
   * Sem handler para o tipo: adia sem contar tentativa (um handler pode chegar numa versão nova).
   */
  public void postpone(long id, Instant nextAttemptAt, String reason) {
    dsl.update(OUTBOX_EVENT)
        .set(OUTBOX_EVENT.NEXT_ATTEMPT_AT, nextAttemptAt.atOffset(ZoneOffset.UTC))
        .set(OUTBOX_EVENT.LAST_ERROR, reason)
        .where(OUTBOX_EVENT.ID.eq(id))
        .execute();
  }

  /** Limpeza diária: processados há mais que {@code keep}. */
  public int deleteProcessedBefore(Instant now, Duration keep) {
    return dsl.deleteFrom(OUTBOX_EVENT)
        .where(OUTBOX_EVENT.PROCESSED_AT.lt(now.minus(keep).atOffset(ZoneOffset.UTC)))
        .execute();
  }
}
