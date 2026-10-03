package br.com.moveup.execution.api;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** Para os jobs diários de alertas (worker): quando cada vínculo treinou e quantas vezes. */
public interface ExecutionActivity {

  /** Fim do último treino finalizado de cada vínculo (vínculo sem treino fica de fora). */
  Map<UUID, Instant> lastFinishedAt(Collection<UUID> linkIds);

  /** Treinos finalizados de cada vínculo com fim em [from, to). */
  Map<UUID, Integer> finishedCount(Collection<UUID> linkIds, Instant from, Instant to);
}
