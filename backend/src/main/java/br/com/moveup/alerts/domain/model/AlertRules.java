package br.com.moveup.alerts.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Quando um fato vira alerta para o profissional (respeitando as configurações dele). Puro: quem
 * chama busca os dados e grava o resultado.
 */
public final class AlertRules {

  /** Abaixo disso a adesão não diz nada (uma falta numa agenda de um treino = 0%). */
  static final int MIN_PLANNED_FOR_ADHERENCE = 2;

  private AlertRules() {}

  /** Para quem vai o alerta: o profissional do vínculo. */
  public record Recipient(UUID professionalId, UUID organizationId, UUID clientId) {}

  /** A sessão em números (o que o aluno escreveu não entra). */
  public record SessionInput(UUID sessionId, Integer effort, boolean hasComment, int painCount) {}

  public static List<NewAlert> forFinishedSession(
      SessionInput session, Recipient to, Map<AlertType, AlertSetting> settings) {
    var alerts = new ArrayList<NewAlert>();
    var id = session.sessionId();
    if (session.painCount() > 0 && on(settings, AlertType.PAIN_REPORTED)) {
      alerts.add(
          alert(to, AlertType.PAIN_REPORTED, Map.of("count", session.painCount()), "pain:" + id));
    }
    var effortLimit = settings.get(AlertType.HIGH_EFFORT).threshold();
    if (session.effort() != null
        && on(settings, AlertType.HIGH_EFFORT)
        && session.effort() >= effortLimit) {
      alerts.add(
          alert(to, AlertType.HIGH_EFFORT, Map.of("effort", session.effort()), "effort:" + id));
    }
    if (session.hasComment() && on(settings, AlertType.NEW_FEEDBACK)) {
      alerts.add(alert(to, AlertType.NEW_FEEDBACK, Map.of(), "feedback:" + id));
    }
    return alerts;
  }

  public static Optional<NewAlert> forEditedSession(
      UUID sessionId, Recipient to, Map<AlertType, AlertSetting> settings) {
    return on(settings, AlertType.SESSION_EDITED)
        ? Optional.of(alert(to, AlertType.SESSION_EDITED, Map.of(), "edited:" + sessionId))
        : Optional.empty();
  }

  public static String clearanceKey(UUID clientId) {
    return "clearance:" + clientId;
  }

  /** Liberação médica pendente (a revisão do personal que libera ou dispensa resolve). */
  public static Optional<NewAlert> clearancePending(
      Recipient to, Map<AlertType, AlertSetting> settings) {
    return on(settings, AlertType.CLEARANCE_PENDING)
        ? Optional.of(alert(to, AlertType.CLEARANCE_PENDING, Map.of(), clearanceKey(to.clientId())))
        : Optional.empty();
  }

  /** Chave do alerta de inatividade (o treino feito o resolve). */
  public static String inactiveKey(UUID clientId) {
    return "inactive:" + clientId;
  }

  public static String adherenceKey(UUID clientId) {
    return "adherence:" + clientId;
  }

  /**
   * @param since último treino finalizado ou, sem treino, o início do vínculo
   */
  public static Optional<NewAlert> inactivity(
      Recipient to, Instant since, Instant now, AlertSetting setting) {
    if (!setting.enabled() || since == null) {
      return Optional.empty();
    }
    var days = (int) Duration.between(since, now).toDays();
    return days >= setting.threshold()
        ? Optional.of(
            alert(to, AlertType.INACTIVE, Map.of("days", days), inactiveKey(to.clientId())))
        : Optional.empty();
  }

  /** Adesão = feitos ÷ previstos pela agenda na janela. */
  public static Optional<NewAlert> adherence(
      Recipient to, int planned, int done, int windowDays, AlertSetting setting) {
    if (!setting.enabled() || planned < MIN_PLANNED_FOR_ADHERENCE) {
      return Optional.empty();
    }
    var percent = Math.min(100, done * 100 / planned);
    return percent < setting.threshold()
        ? Optional.of(
            alert(
                to,
                AlertType.LOW_ADHERENCE,
                Map.of("percent", percent, "planned", planned, "done", done, "days", windowDays),
                adherenceKey(to.clientId())))
        : Optional.empty();
  }

  private static boolean on(Map<AlertType, AlertSetting> settings, AlertType type) {
    return settings.get(type).enabled();
  }

  private static NewAlert alert(
      Recipient to, AlertType type, Map<String, Integer> facts, String key) {
    return new NewAlert(to.professionalId(), to.organizationId(), to.clientId(), type, facts, key);
  }
}
