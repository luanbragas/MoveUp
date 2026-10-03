package br.com.moveup.alerts.application.port.in;

import br.com.moveup.alerts.domain.model.AlertSetting;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Central de atenção do profissional (API). */
public interface ManageAlerts {

  int MAX_PAGE = 50;
  int MAX_SNOOZE_DAYS = 30;

  AlertPage list(UUID professionalId, String status, UUID before, int limit);

  void resolve(UUID professionalId, UUID alertId);

  /** Some da lista por {@code days} dias e volta se o assunto continuar valendo. */
  void snooze(UUID professionalId, UUID alertId, int days);

  List<AlertSetting> settings(UUID professionalId);

  List<AlertSetting> updateSettings(UUID professionalId, List<AlertSetting> changes);

  void registerDevice(UUID userId, String token, String platform);

  void unregisterDevice(UUID userId, String token);

  record AlertItem(
      UUID id,
      String type,
      String severity,
      String status,
      UUID clientId,
      UUID linkId,
      String clientName,
      Map<String, Integer> facts,
      Instant createdAt,
      Instant snoozedUntil) {}

  /**
   * @param next cursor para a próxima página (nulo no fim)
   * @param openCount abertos no total (badge)
   */
  record AlertPage(List<AlertItem> items, UUID next, int openCount) {}
}
