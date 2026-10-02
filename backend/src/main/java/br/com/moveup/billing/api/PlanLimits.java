package br.com.moveup.billing.api;

import java.util.OptionalInt;
import java.util.UUID;

/** Porta pública do billing: limite de alunos ativos do plano vivo da organização. */
public interface PlanLimits {

  /** Limite atual, sem trava (checagem antecipada, ex.: antes de convidar). */
  OptionalInt activeClientLimit(UUID organizationId);

  /**
   * Trava a assinatura viva (FOR UPDATE) até o fim da transação e devolve o limite: duas ativações
   * simultâneas da mesma organização ficam em fila, como no accept_invite. Vazio = sem assinatura
   * viva.
   */
  OptionalInt lockActiveClientLimit(UUID organizationId);
}
