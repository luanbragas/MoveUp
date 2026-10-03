package br.com.moveup.alerts.infrastructure.config;

import br.com.moveup.alerts.application.port.out.AlertSettings;
import br.com.moveup.alerts.application.port.out.Alerts;
import br.com.moveup.alerts.application.port.out.PushDevices;
import br.com.moveup.alerts.application.port.out.PushGateway;
import br.com.moveup.alerts.application.usecase.ClearanceAlertsUseCase;
import br.com.moveup.alerts.application.usecase.DailyAlertsUseCase;
import br.com.moveup.alerts.application.usecase.SendPushesUseCase;
import br.com.moveup.alerts.application.usecase.SessionAlertsUseCase;
import br.com.moveup.alerts.infrastructure.messaging.AnamnesisAlertsHandler;
import br.com.moveup.alerts.infrastructure.messaging.SessionAlertsHandler;
import br.com.moveup.alerts.infrastructure.persistence.JooqPushQueue;
import br.com.moveup.alerts.infrastructure.push.ExpoPushGateway;
import br.com.moveup.anamnesis.api.ClearanceFacts;
import br.com.moveup.coaching.api.CoachingRoster;
import br.com.moveup.execution.api.ExecutionActivity;
import br.com.moveup.execution.api.SessionFacts;
import br.com.moveup.shared.application.outbox.OutboxHandler;
import br.com.moveup.shared.infrastructure.outbox.WorkerLoop;
import br.com.moveup.training.api.TrainingSchedule;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.support.TransactionTemplate;

/** Worker do alerts: alertas por evento e diários, e envio de push. */
@Configuration(proxyBeanMethods = false)
@Profile("worker")
class AlertsWorkerConfig {

  /** Manter na fila só um mês de push enviado ou falho. */
  private static final Duration PUSH_RETENTION = Duration.ofDays(30);

  @Bean
  @ConditionalOnMissingBean(PushGateway.class)
  PushGateway pushGateway(@Value("${moveup.worker.expo-push-url}") URI endpoint) {
    return new ExpoPushGateway(endpoint);
  }

  @Bean
  SessionAlertsUseCase sessionAlerts(
      SessionFacts sessions,
      CoachingRoster roster,
      Alerts alerts,
      AlertSettings settings,
      JooqPushQueue pushes,
      Clock clock) {
    return new SessionAlertsUseCase(sessions, roster, alerts, settings, pushes, clock);
  }

  @Bean
  OutboxHandler sessionAlertsHandler(SessionAlertsUseCase alerts) {
    return new SessionAlertsHandler(alerts);
  }

  @Bean
  OutboxHandler anamnesisAlertsHandler(
      ClearanceFacts clearance,
      CoachingRoster roster,
      Alerts alerts,
      AlertSettings settings,
      JooqPushQueue pushes,
      Clock clock) {
    return new AnamnesisAlertsHandler(
        new ClearanceAlertsUseCase(clearance, roster, alerts, settings, pushes, clock));
  }

  @Bean
  DailyAlertsUseCase dailyAlerts(
      CoachingRoster roster,
      ExecutionActivity activity,
      TrainingSchedule schedule,
      Alerts alerts,
      AlertSettings settings,
      JooqPushQueue pushes,
      Clock clock,
      @Value("${moveup.worker.zone}") ZoneId zone) {
    return new DailyAlertsUseCase(
        roster, activity, schedule, alerts, settings, pushes, clock, zone);
  }

  /** 7h no fuso do produto: o personal vê a lista do dia de manhã. */
  @Bean
  RecurringTask<Void> dailyAlertsTask(
      DailyAlertsUseCase daily, @Value("${moveup.worker.zone}") ZoneId zone) {
    return Tasks.recurring("daily-alerts", Schedules.daily(zone, LocalTime.of(7, 0)))
        .execute((instance, context) -> daily.run());
  }

  @Bean
  SendPushesUseCase sendPushes(
      JooqPushQueue queue,
      PushDevices devices,
      PushGateway gateway,
      TransactionTemplate tx,
      Clock clock,
      @Value("${moveup.worker.push-batch}") int batch,
      @Value("${moveup.worker.push-stale-after}") Duration staleAfter) {
    return new SendPushesUseCase(queue, devices, gateway, tx, clock, batch, staleAfter);
  }

  @Bean
  WorkerLoop pushLoop(
      SendPushesUseCase pushes, @Value("${moveup.worker.outbox-idle}") Duration idle) {
    return new WorkerLoop("push", pushes::sendBatch, idle.multipliedBy(2));
  }

  @Bean
  RecurringTask<Void> pushCleanupTask(JooqPushQueue queue, TransactionTemplate tx, Clock clock) {
    return Tasks.recurring("push-cleanup", Schedules.daily(LocalTime.of(3, 30)))
        .execute(
            (instance, context) ->
                tx.executeWithoutResult(
                    status -> queue.deleteFinishedBefore(clock.instant().minus(PUSH_RETENTION))));
  }
}
