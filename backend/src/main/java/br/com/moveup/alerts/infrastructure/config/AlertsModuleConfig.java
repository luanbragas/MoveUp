package br.com.moveup.alerts.infrastructure.config;

import br.com.moveup.alerts.application.port.in.ManageAlerts;
import br.com.moveup.alerts.application.port.out.AlertSettings;
import br.com.moveup.alerts.application.port.out.Alerts;
import br.com.moveup.alerts.application.port.out.PushDevices;
import br.com.moveup.alerts.application.usecase.ManageAlertsUseCase;
import br.com.moveup.coaching.api.CoachingRoster;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Casos de uso do alerts usados pela API (adaptadores jOOQ são beans do pacote persistence). */
@Configuration(proxyBeanMethods = false)
class AlertsModuleConfig {

  @Bean
  ManageAlerts manageAlerts(
      Alerts alerts,
      AlertSettings settings,
      PushDevices devices,
      CoachingRoster roster,
      Clock clock) {
    return new ManageAlertsUseCase(alerts, settings, devices, roster, clock);
  }
}
