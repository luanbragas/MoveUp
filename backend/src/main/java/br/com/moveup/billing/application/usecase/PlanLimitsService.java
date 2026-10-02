package br.com.moveup.billing.application.usecase;

import br.com.moveup.billing.api.PlanLimits;
import br.com.moveup.billing.application.port.out.SubscriptionLimits;
import java.util.OptionalInt;
import java.util.UUID;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class PlanLimitsService implements PlanLimits {

  private final SubscriptionLimits limits;

  public PlanLimitsService(SubscriptionLimits limits) {
    this.limits = limits;
  }

  @Override
  @Transactional(readOnly = true)
  public OptionalInt activeClientLimit(UUID organizationId) {
    return limits.liveLimit(organizationId, false);
  }

  /** A trava só faz sentido dentro da transação de quem chama. */
  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public OptionalInt lockActiveClientLimit(UUID organizationId) {
    return limits.liveLimit(organizationId, true);
  }
}
