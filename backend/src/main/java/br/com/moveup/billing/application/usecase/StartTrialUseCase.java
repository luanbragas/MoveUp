package br.com.moveup.billing.application.usecase;

import br.com.moveup.billing.api.StartTrial;
import br.com.moveup.billing.application.port.out.TrialSubscriptions;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class StartTrialUseCase implements StartTrial {

  /** Plano semeado na V14 (provisório até a decisão de preços da Fase 8). */
  static final String TRIAL_PLAN = "trial";

  private final TrialSubscriptions subscriptions;
  private final Clock clock;
  private final Duration trialLength;

  public StartTrialUseCase(TrialSubscriptions subscriptions, Clock clock, Duration trialLength) {
    if (trialLength.isNegative() || trialLength.isZero()) {
      throw new IllegalArgumentException("período de teste precisa ser positivo");
    }
    this.subscriptions = subscriptions;
    this.clock = clock;
    this.trialLength = trialLength;
  }

  @Override
  @Transactional
  public void forOrganization(UUID organizationId) {
    subscriptions.startTrial(organizationId, TRIAL_PLAN, clock.instant().plus(trialLength));
  }
}
