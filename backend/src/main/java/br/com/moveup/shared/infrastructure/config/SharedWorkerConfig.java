package br.com.moveup.shared.infrastructure.config;

import br.com.moveup.shared.application.outbox.OutboxHandler;
import br.com.moveup.shared.infrastructure.outbox.OutboxProcessor;
import br.com.moveup.shared.infrastructure.outbox.WorkerLoop;
import br.com.moveup.shared.infrastructure.persistence.JooqOutbox;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Worker: laço do outbox com os handlers de todos os módulos, e a limpeza diária. Conecta como
 * {@code app_worker} (application-worker.yml).
 */
@Configuration(proxyBeanMethods = false)
@Profile("worker")
class SharedWorkerConfig {

  /** Eventos processados ficam 30 dias para investigação (ARQUITETURA 4). */
  private static final Duration OUTBOX_RETENTION = Duration.ofDays(30);

  @Bean
  OutboxProcessor outboxProcessor(
      JooqOutbox outbox, List<OutboxHandler> handlers, TransactionTemplate tx, Clock clock) {
    return new OutboxProcessor(outbox, handlers, tx, clock);
  }

  @Bean
  WorkerLoop outboxLoop(
      OutboxProcessor processor,
      @Value("${moveup.worker.outbox-batch}") int batch,
      @Value("${moveup.worker.outbox-idle}") Duration idle) {
    return new WorkerLoop("outbox", () -> processor.processAvailable(batch), idle);
  }

  @Bean
  RecurringTask<Void> outboxCleanupTask(JooqOutbox outbox, TransactionTemplate tx, Clock clock) {
    return Tasks.recurring("outbox-cleanup", Schedules.daily(LocalTime.of(3, 0)))
        .execute(
            (instance, context) ->
                tx.executeWithoutResult(
                    status -> outbox.deleteProcessedBefore(clock.instant(), OUTBOX_RETENTION)));
  }
}
