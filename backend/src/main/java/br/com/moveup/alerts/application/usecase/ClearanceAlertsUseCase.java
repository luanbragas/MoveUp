package br.com.moveup.alerts.application.usecase;

import br.com.moveup.alerts.application.port.out.AlertSettings;
import br.com.moveup.alerts.application.port.out.Alerts;
import br.com.moveup.alerts.application.port.out.PushQueue;
import br.com.moveup.alerts.domain.model.AlertRules;
import br.com.moveup.anamnesis.api.ClearanceFacts;
import br.com.moveup.coaching.api.CoachingRoster;
import java.time.Clock;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * Liberação médica (worker, eventos da anamnese): pendente na versão mais recente abre o alerta;
 * liberada ou dispensada pelo personal o resolve.
 */
public class ClearanceAlertsUseCase {

  private final ClearanceFacts clearance;
  private final CoachingRoster roster;
  private final Alerts alerts;
  private final RaiseAlerts raise;
  private final Clock clock;

  public ClearanceAlertsUseCase(
      ClearanceFacts clearance,
      CoachingRoster roster,
      Alerts alerts,
      AlertSettings settings,
      PushQueue pushes,
      Clock clock) {
    this.clearance = clearance;
    this.roster = roster;
    this.alerts = alerts;
    this.raise = new RaiseAlerts(alerts, settings, pushes);
    this.clock = clock;
  }

  @Transactional
  public void changed(UUID anamnesisId) {
    clearance
        .latestFor(anamnesisId)
        .filter(state -> state.linkId() != null)
        .ifPresent(
            state ->
                roster
                    .owner(state.linkId())
                    .ifPresent(
                        owner -> {
                          var to =
                              new AlertRules.Recipient(
                                  owner.professionalId(), owner.organizationId(), state.clientId());
                          if (state.pending()) {
                            var settings = raise.settingsOf(owner.professionalId());
                            AlertRules.clearancePending(to, settings)
                                .ifPresent(alert -> raise.raise(alert, settings));
                          } else {
                            alerts.resolveOpen(
                                owner.professionalId(),
                                AlertRules.clearanceKey(state.clientId()),
                                clock.instant());
                          }
                        }));
  }
}
