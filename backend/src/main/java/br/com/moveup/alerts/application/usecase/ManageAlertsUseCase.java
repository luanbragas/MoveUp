package br.com.moveup.alerts.application.usecase;

import br.com.moveup.alerts.application.port.in.ManageAlerts;
import br.com.moveup.alerts.application.port.out.AlertSettings;
import br.com.moveup.alerts.application.port.out.Alerts;
import br.com.moveup.alerts.application.port.out.PushDevices;
import br.com.moveup.alerts.domain.exception.InvalidAlertData;
import br.com.moveup.alerts.domain.model.AlertSetting;
import br.com.moveup.coaching.api.CoachingRoster;
import br.com.moveup.shared.domain.ResourceNotFound;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class ManageAlertsUseCase implements ManageAlerts {

  private static final Set<String> STATUSES = Set.of("open", "snoozed", "resolved");
  private static final Set<String> PLATFORMS = Set.of("ios", "android");

  private final Alerts alerts;
  private final AlertSettings settings;
  private final PushDevices devices;
  private final CoachingRoster roster;
  private final Clock clock;

  public ManageAlertsUseCase(
      Alerts alerts,
      AlertSettings settings,
      PushDevices devices,
      CoachingRoster roster,
      Clock clock) {
    this.alerts = alerts;
    this.settings = settings;
    this.devices = devices;
    this.roster = roster;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public AlertPage list(UUID professionalId, String status, UUID before, int limit) {
    var filter = status == null ? "open" : status;
    if (!STATUSES.contains(filter)) {
      throw new InvalidAlertData("alert-status-invalid", "Filtro de status inválido.");
    }
    var size = Math.clamp(limit, 1, MAX_PAGE);
    var now = clock.instant();
    var page = alerts.list(professionalId, filter, before, size + 1, now);
    var visible = page.subList(0, Math.min(size, page.size()));
    var clients =
        roster.clientsOf(
            professionalId, visible.stream().map(Alerts.AlertView::clientId).distinct().toList());
    var items =
        visible.stream()
            .map(
                a -> {
                  var client = clients.get(a.clientId());
                  return new AlertItem(
                      a.id(),
                      a.type(),
                      a.severity(),
                      a.status(),
                      a.clientId(),
                      client == null ? null : client.linkId(),
                      client == null ? null : client.name(),
                      a.facts(),
                      a.createdAt(),
                      a.snoozedUntil());
                })
            .toList();
    var next = page.size() > size ? visible.getLast().id() : null;
    return new AlertPage(items, next, alerts.countOpen(professionalId, now));
  }

  @Override
  @Transactional
  public void resolve(UUID professionalId, UUID alertId) {
    var alert = alerts.find(professionalId, alertId).orElseThrow(ResourceNotFound::new);
    if (!"resolved".equals(alert.status())) {
      alerts.resolve(alertId, clock.instant());
    }
  }

  @Override
  @Transactional
  public void snooze(UUID professionalId, UUID alertId, int days) {
    if (days < 1 || days > MAX_SNOOZE_DAYS) {
      throw new InvalidAlertData("snooze-invalid", "Adie de 1 a 30 dias.");
    }
    var alert = alerts.find(professionalId, alertId).orElseThrow(ResourceNotFound::new);
    if ("resolved".equals(alert.status())) {
      throw new InvalidAlertData("alert-resolved", "Este alerta já foi resolvido.");
    }
    alerts.snooze(alertId, clock.instant().plus(Duration.ofDays(days)));
  }

  @Override
  @Transactional(readOnly = true)
  public List<AlertSetting> settings(UUID professionalId) {
    return List.copyOf(AlertSetting.withDefaults(settings.stored(professionalId)).values());
  }

  @Override
  @Transactional
  public List<AlertSetting> updateSettings(UUID professionalId, List<AlertSetting> changes) {
    changes.forEach(s -> settings.save(professionalId, s));
    return settings(professionalId);
  }

  @Override
  @Transactional
  public void registerDevice(UUID userId, String token, String platform) {
    if (!PLATFORMS.contains(platform)) {
      throw new InvalidAlertData("platform-invalid", "Plataforma inválida.");
    }
    if (token == null || !token.matches("Expo(nent)?PushToken\\[[A-Za-z0-9_-]{8,128}]")) {
      throw new InvalidAlertData("push-token-invalid", "Token de push inválido.");
    }
    devices.register(userId, token, platform, clock.instant());
  }

  @Override
  @Transactional
  public void unregisterDevice(UUID userId, String token) {
    devices.unregister(userId, token);
  }
}
