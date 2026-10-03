package br.com.moveup.support;

import br.com.moveup.alerts.application.port.out.PushGateway;
import br.com.moveup.alerts.application.usecase.DailyAlertsUseCase;
import br.com.moveup.alerts.application.usecase.SendPushesUseCase;
import br.com.moveup.alerts.application.usecase.SessionAlertsUseCase;
import br.com.moveup.alerts.infrastructure.messaging.SessionAlertsHandler;
import br.com.moveup.alerts.infrastructure.persistence.JooqAlertSettings;
import br.com.moveup.alerts.infrastructure.persistence.JooqAlerts;
import br.com.moveup.alerts.infrastructure.persistence.JooqPushDevices;
import br.com.moveup.alerts.infrastructure.persistence.JooqPushQueue;
import br.com.moveup.coaching.infrastructure.persistence.JooqCoachingRoster;
import br.com.moveup.db.PostgresTestDatabase;
import br.com.moveup.execution.application.usecase.RecalculateRecordsUseCase;
import br.com.moveup.execution.infrastructure.messaging.SessionRecordsHandler;
import br.com.moveup.execution.infrastructure.persistence.JooqExecutionFacts;
import br.com.moveup.execution.infrastructure.persistence.JooqRecordHistory;
import br.com.moveup.shared.application.outbox.OutboxHandler;
import br.com.moveup.shared.infrastructure.outbox.OutboxProcessor;
import br.com.moveup.shared.infrastructure.persistence.JooqOutbox;
import br.com.moveup.shared.infrastructure.persistence.RlsTransactionManager;
import br.com.moveup.training.infrastructure.persistence.JooqTrainingSchedule;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.DataSourceConnectionProvider;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * As peças do worker montadas como em produção (mesmas classes, mesma transação com {@code
 * set_config}), conectadas ao banco de teste como {@code app_worker}. Os testes chamam um passo de
 * cada vez em vez de subir os laços.
 */
public final class WorkerHarness {

  public final DSLContext dsl;
  public final TransactionTemplate tx;
  public final JooqPushQueue pushQueue;
  public final List<OutboxHandler> handlers = new ArrayList<>();
  private final Clock clock;

  public WorkerHarness(Clock clock) {
    this.clock = clock;
    var ds =
        new DriverManagerDataSource(
            PostgresTestDatabase.jdbcUrl(),
            PostgresTestDatabase.workerUser(),
            PostgresTestDatabase.workerPassword());
    this.tx = new TransactionTemplate(new RlsTransactionManager(ds, Optional::empty));
    this.dsl =
        DSL.using(
            new DataSourceConnectionProvider(new TransactionAwareDataSourceProxy(ds)),
            SQLDialect.POSTGRES);
    this.pushQueue = new JooqPushQueue(dsl);
    handlers.add(
        new SessionRecordsHandler(new RecalculateRecordsUseCase(new JooqRecordHistory(dsl))));
    handlers.add(new SessionAlertsHandler(sessionAlerts()));
  }

  public SessionAlertsUseCase sessionAlerts() {
    return new SessionAlertsUseCase(
        new JooqExecutionFacts(dsl),
        new JooqCoachingRoster(dsl),
        new JooqAlerts(dsl),
        new JooqAlertSettings(dsl),
        pushQueue,
        clock);
  }

  /** Processador do outbox com os handlers atuais (o teste pode acrescentar um que falha). */
  public OutboxProcessor outbox() {
    return new OutboxProcessor(new JooqOutbox(dsl), handlers, tx, clock);
  }

  public DailyAlertsUseCase daily() {
    var facts = new JooqExecutionFacts(dsl);
    return new DailyAlertsUseCase(
        new JooqCoachingRoster(dsl),
        facts,
        new JooqTrainingSchedule(dsl),
        new JooqAlerts(dsl),
        new JooqAlertSettings(dsl),
        pushQueue,
        clock,
        ZoneId.of("America/Sao_Paulo"));
  }

  public SendPushesUseCase pushes(PushGateway gateway, Duration staleAfter) {
    // a fila de push compara com a hora certa (o relógio atrasado é só para o outbox)
    return new SendPushesUseCase(
        pushQueue, new JooqPushDevices(dsl), gateway, tx, Clock.systemUTC(), 50, staleAfter);
  }
}
