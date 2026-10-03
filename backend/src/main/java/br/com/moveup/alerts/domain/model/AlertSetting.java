package br.com.moveup.alerts.domain.model;

import java.util.EnumMap;
import java.util.Map;

/**
 * Configuração de um tipo de alerta para o profissional: ligado, com push, e o limite (dias sem
 * treinar, esforço, adesão). Sem linha no banco vale o padrão do tipo.
 */
public record AlertSetting(AlertType type, boolean enabled, boolean push, Integer threshold) {

  public AlertSetting {
    type.checkThreshold(threshold);
  }

  /** As configurações gravadas por cima dos padrões, uma por tipo. */
  public static Map<AlertType, AlertSetting> withDefaults(Map<AlertType, AlertSetting> stored) {
    var all = new EnumMap<AlertType, AlertSetting>(AlertType.class);
    for (var type : AlertType.values()) {
      all.put(type, stored.getOrDefault(type, type.defaults()));
    }
    return all;
  }
}
