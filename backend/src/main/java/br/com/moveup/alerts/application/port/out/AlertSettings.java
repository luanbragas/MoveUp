package br.com.moveup.alerts.application.port.out;

import br.com.moveup.alerts.domain.model.AlertSetting;
import br.com.moveup.alerts.domain.model.AlertType;
import java.util.Map;
import java.util.UUID;

/** Configurações gravadas pelo profissional (sem linha = padrão do tipo). */
public interface AlertSettings {

  Map<AlertType, AlertSetting> stored(UUID professionalId);

  void save(UUID professionalId, AlertSetting setting);
}
