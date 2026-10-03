package br.com.moveup.alerts.domain.model;

import br.com.moveup.alerts.domain.exception.InvalidAlertData;
import java.util.Locale;

/**
 * Tipos de alerta que o sistema gera hoje, com o padrão de cada um. Avaliação atrasada chega com a
 * Fase 7 (o banco já aceita o código).
 */
public enum AlertType {
  PAIN_REPORTED(Severity.URGENT, true, null, null),
  HIGH_EFFORT(Severity.WARNING, false, 9, new int[] {5, 10}),
  NEW_FEEDBACK(Severity.INFO, false, null, null),
  SESSION_EDITED(Severity.INFO, false, null, null),
  /** Limite: dias sem treinar. */
  INACTIVE(Severity.WARNING, true, 7, new int[] {2, 60}),
  /** Limite: adesão mínima em % nos últimos 14 dias. */
  LOW_ADHERENCE(Severity.WARNING, false, 50, new int[] {10, 100}),
  /** PAR-Q com "sim" e liberação médica ainda pendente na anamnese. */
  CLEARANCE_PENDING(Severity.WARNING, false, null, null);

  private final Severity severity;
  private final boolean defaultPush;
  private final Integer defaultThreshold;
  private final int[] thresholdRange;

  AlertType(Severity severity, boolean defaultPush, Integer defaultThreshold, int[] range) {
    this.severity = severity;
    this.defaultPush = defaultPush;
    this.defaultThreshold = defaultThreshold;
    this.thresholdRange = range;
  }

  public String code() {
    return name().toLowerCase(Locale.ROOT);
  }

  public Severity severity() {
    return severity;
  }

  public boolean hasThreshold() {
    return defaultThreshold != null;
  }

  public AlertSetting defaults() {
    return new AlertSetting(this, true, defaultPush, defaultThreshold);
  }

  void checkThreshold(Integer value) {
    if (thresholdRange == null) {
      if (value != null) {
        throw new InvalidAlertData("threshold-invalid", "Este alerta não tem limite.");
      }
      return;
    }
    if (value == null || value < thresholdRange[0] || value > thresholdRange[1]) {
      throw new InvalidAlertData(
          "threshold-invalid",
          "Limite entre " + thresholdRange[0] + " e " + thresholdRange[1] + ".");
    }
  }

  public static AlertType fromCode(String code) {
    for (var type : values()) {
      if (type.code().equals(code)) {
        return type;
      }
    }
    throw new InvalidAlertData("alert-type-invalid", "Tipo de alerta desconhecido.");
  }

  public enum Severity {
    INFO,
    WARNING,
    URGENT;

    public String code() {
      return name().toLowerCase(Locale.ROOT);
    }
  }
}
