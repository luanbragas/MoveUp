package br.com.moveup.alerts;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.moveup.db.PostgresTestDatabase;
import br.com.moveup.shared.application.outbox.OutboxHandler;
import br.com.moveup.shared.infrastructure.outbox.OutboxProcessor;
import br.com.moveup.shared.infrastructure.outbox.WorkerLoop;
import br.com.moveup.support.TestJwt;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * O perfil worker sobe como em produção: conectado como app_worker, com os handlers dos módulos, os
 * laços (outbox e push) e os jobs do db-scheduler. Fecha no fim para os laços não continuarem
 * rodando durante os outros testes.
 */
@SpringBootTest
@ActiveProfiles("worker")
@DirtiesContext
class WorkerContextTest {

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    TestJwt.registerProperties(registry);
    registry.add("spring.datasource.username", PostgresTestDatabase::workerUser);
    registry.add("spring.datasource.password", PostgresTestDatabase::workerPassword);
  }

  @Autowired List<OutboxHandler> handlers;
  @Autowired OutboxProcessor processor;
  @Autowired List<WorkerLoop> loops;
  @Autowired List<RecurringTask<?>> tasks;

  @Test
  void workerSobeComHandlersLacosEJobs() {
    assertThat(handlers).hasSize(2);
    assertThat(loops).hasSize(2).allMatch(WorkerLoop::isRunning);
    assertThat(tasks)
        .extracting(t -> t.getName())
        .containsExactlyInAnyOrder("daily-alerts", "outbox-cleanup", "push-cleanup");
    // a fila pode ser lida como app_worker (sem erro de permissão)
    assertThat(processor.processAvailable(0)).isZero();
  }
}
