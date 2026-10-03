package br.com.moveup.execution.api;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Para o módulo alerts (worker): o resumo de uma sessão em números e sim/não. Nada de texto do
 * aluno nem região de dor: o alerta só diz que existe algo para ver.
 */
public interface SessionFacts {

  Optional<SessionSummary> summary(UUID sessionId);

  /**
   * @param effort esforço 0–10 do feedback (nulo sem feedback)
   * @param painCount dores relatadas na sessão
   */
  record SessionSummary(
      UUID sessionId,
      UUID clientId,
      UUID linkId,
      Integer effort,
      boolean hasComment,
      int painCount,
      Instant finishedAt) {}
}
