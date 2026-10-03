package br.com.moveup.alerts.application.usecase;

import br.com.moveup.alerts.application.port.out.AlertSettings;
import br.com.moveup.alerts.application.port.out.Alerts;
import br.com.moveup.alerts.application.port.out.PushQueue;
import br.com.moveup.alerts.domain.model.AlertRules;
import br.com.moveup.alerts.domain.model.AlertSetting;
import br.com.moveup.alerts.domain.model.AlertType;
import br.com.moveup.coaching.api.CoachingRoster;
import br.com.moveup.execution.api.ExecutionActivity;
import br.com.moveup.training.api.TrainingSchedule;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * Job diário (worker): inatividade e adesão baixa de cada vínculo ativo, e adiados vencidos voltam
 * a abertos. Idempotente: rodar duas vezes no dia não duplica (deduplicação pela chave), e o que
 * deixou de valer é resolvido.
 */
public class DailyAlertsUseCase {

  /** Janela da adesão: as duas últimas semanas completas até ontem. */
  static final int ADHERENCE_WINDOW_DAYS = 14;

  private final CoachingRoster roster;
  private final ExecutionActivity activity;
  private final TrainingSchedule schedule;
  private final Alerts alerts;
  private final RaiseAlerts raise;
  private final Clock clock;
  private final ZoneId zone;

  public DailyAlertsUseCase(
      CoachingRoster roster,
      ExecutionActivity activity,
      TrainingSchedule schedule,
      Alerts alerts,
      AlertSettings settings,
      PushQueue pushes,
      Clock clock,
      ZoneId zone) {
    this.roster = roster;
    this.activity = activity;
    this.schedule = schedule;
    this.alerts = alerts;
    this.raise = new RaiseAlerts(alerts, settings, pushes);
    this.clock = clock;
    this.zone = zone;
  }

  @Transactional
  public void run() {
    var now = clock.instant();
    alerts.wakeSnoozed(now);
    var links = roster.activeLinks();
    var ids = links.stream().map(CoachingRoster.LinkOwner::linkId).toList();
    var today = LocalDate.ofInstant(now, zone);
    var to = today.minusDays(1);
    var from = to.minusDays(ADHERENCE_WINDOW_DAYS - 1L);
    var last = activity.lastFinishedAt(ids);
    var done =
        activity.finishedCount(
            ids, from.atStartOfDay(zone).toInstant(), today.atStartOfDay(zone).toInstant());
    var planned = schedule.plannedSessions(ids, from, to);
    var settingsCache = new HashMap<UUID, Map<AlertType, AlertSetting>>();

    for (var link : links) {
      var settings = settingsCache.computeIfAbsent(link.professionalId(), raise::settingsOf);
      var recipient =
          new AlertRules.Recipient(link.professionalId(), link.organizationId(), link.clientId());
      var since = last.getOrDefault(link.linkId(), link.startedAt());
      AlertRules.inactivity(recipient, since, now, settings.get(AlertType.INACTIVE))
          .ifPresentOrElse(
              alert -> raise.raise(alert, settings),
              () ->
                  alerts.resolveOpen(
                      link.professionalId(), AlertRules.inactiveKey(link.clientId()), now));
      AlertRules.adherence(
              recipient,
              planned.getOrDefault(link.linkId(), 0),
              done.getOrDefault(link.linkId(), 0),
              ADHERENCE_WINDOW_DAYS,
              settings.get(AlertType.LOW_ADHERENCE))
          .ifPresentOrElse(
              alert -> raise.raise(alert, settings),
              () ->
                  alerts.resolveOpen(
                      link.professionalId(), AlertRules.adherenceKey(link.clientId()), now));
    }
  }
}
