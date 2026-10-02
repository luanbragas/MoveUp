package br.com.moveup.billing.infrastructure.config;

import br.com.moveup.billing.api.StartTrial;
import br.com.moveup.billing.application.port.out.TrialSubscriptions;
import br.com.moveup.billing.application.usecase.StartTrialUseCase;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class BillingModuleConfig {

  @Bean
  StartTrial startTrial(
      TrialSubscriptions subscriptions,
      Clock clock,
      @Value("${moveup.billing.trial-length:P14D}") Duration trialLength) {
    return new StartTrialUseCase(subscriptions, clock, trialLength);
  }
}
