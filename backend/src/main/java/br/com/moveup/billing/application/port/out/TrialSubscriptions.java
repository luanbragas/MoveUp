package br.com.moveup.billing.application.port.out;

import java.time.Instant;
import java.util.UUID;

public interface TrialSubscriptions {

  /** Cria a assinatura {@code trialing} no plano de teste. */
  void startTrial(UUID organizationId, String planCode, Instant trialEndsAt);
}
