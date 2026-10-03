package br.com.moveup.alerts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.moveup.alerts.domain.exception.InvalidAlertData;
import br.com.moveup.alerts.domain.model.AlertRules;
import br.com.moveup.alerts.domain.model.AlertSetting;
import br.com.moveup.alerts.domain.model.AlertType;
import br.com.moveup.alerts.domain.model.NewAlert;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AlertRulesTest {

  private static final AlertRules.Recipient TO =
      new AlertRules.Recipient(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
  private static final Map<AlertType, AlertSetting> DEFAULTS = AlertSetting.withDefaults(Map.of());

  @Test
  void sessaoComDorEsforcoAltoEComentarioGeraTresAlertasSoComNumeros() {
    var id = UUID.randomUUID();
    var alerts =
        AlertRules.forFinishedSession(new AlertRules.SessionInput(id, 9, true, 2), TO, DEFAULTS);

    assertThat(alerts)
        .extracting(NewAlert::type)
        .containsExactly(AlertType.PAIN_REPORTED, AlertType.HIGH_EFFORT, AlertType.NEW_FEEDBACK);
    assertThat(alerts.getFirst().facts()).containsEntry("count", 2);
    assertThat(alerts.getFirst().dedupeKey()).isEqualTo("pain:" + id);
  }

  @Test
  void configuracaoDesligadaOuLimiteMaiorNaoAlerta() {
    var settings =
        AlertSetting.withDefaults(
            Map.of(
                AlertType.HIGH_EFFORT, new AlertSetting(AlertType.HIGH_EFFORT, true, false, 10),
                AlertType.PAIN_REPORTED,
                    new AlertSetting(AlertType.PAIN_REPORTED, false, false, null)));
    assertThat(
            AlertRules.forFinishedSession(
                new AlertRules.SessionInput(UUID.randomUUID(), 9, false, 1), TO, settings))
        .isEmpty();
  }

  @Test
  void inatividadePeloLimiteDeDias() {
    var now = Instant.parse("2026-10-20T10:00:00Z");
    var setting = AlertType.INACTIVE.defaults();
    assertThat(AlertRules.inactivity(TO, now.minus(Duration.ofDays(6)), now, setting)).isEmpty();
    assertThat(AlertRules.inactivity(TO, now.minus(Duration.ofDays(8)), now, setting))
        .get()
        .extracting(a -> a.facts().get("days"))
        .isEqualTo(8);
  }

  @Test
  void adesaoAbaixoDoLimiteComAgendaSuficiente() {
    var setting = AlertType.LOW_ADHERENCE.defaults();
    assertThat(AlertRules.adherence(TO, 1, 0, 14, setting)).isEmpty(); // agenda curta demais
    assertThat(AlertRules.adherence(TO, 6, 3, 14, setting)).isEmpty(); // 50% = no limite
    assertThat(AlertRules.adherence(TO, 6, 2, 14, setting))
        .get()
        .extracting(a -> a.facts().get("percent"))
        .isEqualTo(33);
  }

  @Test
  void limiteForaDaFaixaEhRecusado() {
    assertThatThrownBy(() -> new AlertSetting(AlertType.INACTIVE, true, true, 1))
        .isInstanceOf(InvalidAlertData.class);
    assertThatThrownBy(() -> new AlertSetting(AlertType.NEW_FEEDBACK, true, true, 3))
        .isInstanceOf(InvalidAlertData.class);
  }
}
