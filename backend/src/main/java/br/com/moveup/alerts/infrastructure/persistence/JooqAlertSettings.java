package br.com.moveup.alerts.infrastructure.persistence;

import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.ALERT_SETTING;

import br.com.moveup.alerts.application.port.out.AlertSettings;
import br.com.moveup.alerts.domain.model.AlertSetting;
import br.com.moveup.alerts.domain.model.AlertType;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.springframework.stereotype.Repository;

/** Configurações de alerta (sem RLS: sempre filtradas pelo profissional autenticado). */
@Repository
public class JooqAlertSettings implements AlertSettings {

  private final DSLContext dsl;

  public JooqAlertSettings(DSLContext dsl) {
    this.dsl = dsl;
  }

  @Override
  public Map<AlertType, AlertSetting> stored(UUID professionalId) {
    var out = new EnumMap<AlertType, AlertSetting>(AlertType.class);
    dsl.selectFrom(ALERT_SETTING)
        .where(ALERT_SETTING.PROFESSIONAL_ID.eq(professionalId))
        .forEach(
            r -> {
              AlertType type;
              try {
                type = AlertType.fromCode(r.getType());
              } catch (RuntimeException unknown) {
                return; // tipo que esta versão ainda não gera
              }
              out.put(
                  type,
                  new AlertSetting(
                      type,
                      r.getEnabled(),
                      r.getPushEnabled(),
                      type.hasThreshold() ? threshold(r.getThreshold(), type) : null));
            });
    return out;
  }

  @Override
  public void save(UUID professionalId, AlertSetting s) {
    var threshold =
        s.threshold() == null ? null : JSONB.valueOf("{\"value\": " + s.threshold() + "}");
    dsl.insertInto(ALERT_SETTING)
        .set(ALERT_SETTING.PROFESSIONAL_ID, professionalId)
        .set(ALERT_SETTING.TYPE, s.type().code())
        .set(ALERT_SETTING.ENABLED, s.enabled())
        .set(ALERT_SETTING.PUSH_ENABLED, s.push())
        .set(ALERT_SETTING.THRESHOLD, threshold)
        .onConflict(ALERT_SETTING.PROFESSIONAL_ID, ALERT_SETTING.TYPE)
        .doUpdate()
        .set(ALERT_SETTING.ENABLED, s.enabled())
        .set(ALERT_SETTING.PUSH_ENABLED, s.push())
        .set(ALERT_SETTING.THRESHOLD, threshold)
        .execute();
  }

  private static Integer threshold(JSONB json, AlertType type) {
    if (json == null) {
      return type.defaults().threshold();
    }
    var matcher = java.util.regex.Pattern.compile("\"value\"\\s*:\\s*(\\d+)").matcher(json.data());
    return matcher.find() ? Integer.valueOf(matcher.group(1)) : type.defaults().threshold();
  }
}
