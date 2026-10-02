package br.com.moveup.billing.application.port.out;

import java.util.OptionalInt;
import java.util.UUID;

public interface SubscriptionLimits {

  OptionalInt liveLimit(UUID organizationId, boolean lock);
}
