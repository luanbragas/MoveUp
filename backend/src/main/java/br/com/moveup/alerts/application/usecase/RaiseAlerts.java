package br.com.moveup.alerts.application.usecase;

import br.com.moveup.alerts.application.port.out.AlertSettings;
import br.com.moveup.alerts.application.port.out.Alerts;
import br.com.moveup.alerts.application.port.out.PushQueue;
import br.com.moveup.alerts.domain.model.AlertSetting;
import br.com.moveup.alerts.domain.model.AlertType;
import br.com.moveup.alerts.domain.model.NewAlert;
import br.com.moveup.alerts.domain.model.PushText;
import java.util.Map;
import java.util.UUID;

/**
 * Grava um alerta (se não houver um aberto igual) e, se o profissional quer push para o tipo, põe o
 * push na fila na mesma transação. Compartilhado pelos alertas de evento e dos jobs.
 */
class RaiseAlerts {

  private final Alerts alerts;
  private final AlertSettings settings;
  private final PushQueue pushes;

  RaiseAlerts(Alerts alerts, AlertSettings settings, PushQueue pushes) {
    this.alerts = alerts;
    this.settings = settings;
    this.pushes = pushes;
  }

  Map<AlertType, AlertSetting> settingsOf(UUID professionalId) {
    return AlertSetting.withDefaults(settings.stored(professionalId));
  }

  void raise(NewAlert alert, Map<AlertType, AlertSetting> professionalSettings) {
    alerts
        .openIfNew(alert)
        .filter(id -> professionalSettings.get(alert.type()).push())
        .ifPresent(
            id ->
                pushes.enqueue(
                    alert.professionalId(),
                    "alert:" + id,
                    PushText.TITLE,
                    PushText.BODY,
                    Map.of("alertId", id.toString())));
  }
}
