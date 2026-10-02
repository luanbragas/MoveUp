package br.com.moveup.billing.api;

import java.util.UUID;

/**
 * Porta pública do billing: toda organização nova começa no período de teste (Fase 1). Roda na
 * transação de quem chama (cadastro do profissional).
 */
public interface StartTrial {

  void forOrganization(UUID organizationId);
}
