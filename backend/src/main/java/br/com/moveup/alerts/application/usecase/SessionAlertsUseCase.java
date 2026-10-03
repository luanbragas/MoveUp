package br.com.moveup.alerts.application.usecase;

import br.com.moveup.alerts.application.port.out.AlertSettings;
import br.com.moveup.alerts.application.port.out.Alerts;
import br.com.moveup.alerts.application.port.out.PushQueue;
import br.com.moveup.alerts.domain.model.AlertRules;
import br.com.moveup.coaching.api.CoachingRoster;
import br.com.moveup.execution.api.SessionFacts;
import java.time.Clock;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * Alertas que nascem de uma sessão (worker, dentro da transação do evento): dor, esforço alto e
 * comentário ao finalizar; correção depois de finalizada. Treinar resolve a inatividade aberta.
 */
public class SessionAlertsUseCase {

  private final SessionFacts sessions;
  private final CoachingRoster roster;
  private final Alerts alerts;
  private final RaiseAlerts raise;
  private final Clock clock;

  public SessionAlertsUseCase(
      SessionFacts sessions,
      CoachingRoster roster,
      Alerts alerts,
      AlertSettings settings,
      PushQueue pushes,
      Clock clock) {
    this.sessions = sessions;
    this.roster = roster;
    this.alerts = alerts;
    this.raise = new RaiseAlerts(alerts, settings, pushes);
    this.clock = clock;
  }

  @Transactional
  public void finished(UUID sessionId) {
    sessions
        .summary(sessionId)
        .ifPresent(
            s ->
                roster
                    .owner(s.linkId())
                    .ifPresent(
                        owner -> {
                          var to =
                              new AlertRules.Recipient(
                                  owner.professionalId(), owner.organizationId(), s.clientId());
                          var settings = raise.settingsOf(owner.professionalId());
                          AlertRules.forFinishedSession(
                                  new AlertRules.SessionInput(
                                      s.sessionId(), s.effort(), s.hasComment(), s.painCount()),
                                  to,
                                  settings)
                              .forEach(alert -> raise.raise(alert, settings));
                          alerts.resolveOpen(
                              owner.professionalId(),
                              AlertRules.inactiveKey(s.clientId()),
                              clock.instant());
                        }));
  }

  @Transactional
  public void edited(UUID sessionId) {
    sessions
        .summary(sessionId)
        .ifPresent(
            s ->
                roster
                    .owner(s.linkId())
                    .ifPresent(
                        owner -> {
                          var settings = raise.settingsOf(owner.professionalId());
                          AlertRules.forEditedSession(
                                  sessionId,
                                  new AlertRules.Recipient(
                                      owner.professionalId(), owner.organizationId(), s.clientId()),
                                  settings)
                              .ifPresent(alert -> raise.raise(alert, settings));
                        }));
  }
}
