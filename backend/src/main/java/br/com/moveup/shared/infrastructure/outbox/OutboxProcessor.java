package br.com.moveup.shared.infrastructure.outbox;

import br.com.moveup.shared.application.outbox.OutboxEvent;
import br.com.moveup.shared.application.outbox.OutboxHandler;
import br.com.moveup.shared.infrastructure.persistence.JooqOutbox;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Processa o outbox um evento por transação: trava o evento, roda os handlers do tipo e marca como
 * processado no mesmo commit. Se qualquer handler falhar (ou o worker cair), a transação volta
 * inteira e o evento é tentado de novo com backoff: nada se perde e nada grava duas vezes.
 */
public class OutboxProcessor {

  private static final Logger LOG = LoggerFactory.getLogger(OutboxProcessor.class);
  private static final Duration FIRST_RETRY = Duration.ofSeconds(5);
  private static final Duration MAX_RETRY = Duration.ofHours(1);
  private static final int MAX_FAST_ATTEMPTS = 10;
  private static final Duration NO_HANDLER_RETRY = Duration.ofHours(1);

  private final JooqOutbox outbox;
  private final List<OutboxHandler> handlers;
  private final TransactionTemplate tx;
  private final Clock clock;

  public OutboxProcessor(
      JooqOutbox outbox, List<OutboxHandler> handlers, TransactionTemplate tx, Clock clock) {
    this.outbox = outbox;
    this.handlers = List.copyOf(handlers);
    this.tx = tx;
    this.clock = clock;
  }

  /** Processa até {@code max} eventos vencidos; devolve quantos pegou (0 = fila em dia). */
  public int processAvailable(int max) {
    var taken = 0;
    while (taken < max && processOne()) {
      taken++;
    }
    return taken;
  }

  /** Um evento, numa transação. Falso quando não há evento vencido. */
  public boolean processOne() {
    var claimed = new AtomicReference<OutboxEvent>();
    try {
      return Boolean.TRUE.equals(
          tx.execute(
              status -> {
                var next = outbox.lockNext();
                if (next.isEmpty()) {
                  return false;
                }
                var event = next.get();
                claimed.set(event);
                var matching = handlers.stream().filter(h -> h.types().contains(event.type()));
                var list = matching.toList();
                if (list.isEmpty()) {
                  outbox.postpone(event.id(), now().plus(NO_HANDLER_RETRY), "no-handler");
                  LOG.warn("outbox_no_handler id={} type={}", event.id(), event.type());
                  return true;
                }
                list.forEach(h -> h.handle(event));
                outbox.markProcessed(event.id());
                return true;
              }));
    } catch (RuntimeException e) {
      var event = claimed.get();
      if (event == null) {
        throw e;
      }
      var attempts = event.attempts() + 1;
      // só o tipo do erro: a mensagem pode trazer dado do aluno (CLAUDE.md, regra 7)
      var error = e.getClass().getSimpleName();
      tx.executeWithoutResult(
          status -> outbox.markFailed(event.id(), attempts, now().plus(backoff(attempts)), error));
      LOG.warn(
          "outbox_failed id={} type={} attempts={} error={}",
          event.id(),
          event.type(),
          attempts,
          error);
      return true;
    }
  }

  /** 5 s, 10 s, 20 s… até 1 h; depois de 10 tentativas, uma vez por dia. */
  static Duration backoff(int attempts) {
    if (attempts > MAX_FAST_ATTEMPTS) {
      return Duration.ofDays(1);
    }
    var delay = FIRST_RETRY.multipliedBy(1L << Math.min(attempts - 1, 20));
    return delay.compareTo(MAX_RETRY) > 0 ? MAX_RETRY : delay;
  }

  private Instant now() {
    return clock.instant();
  }
}
