package br.com.moveup.execution.application.port.out;

import br.com.moveup.execution.domain.model.PersonalRecords;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Séries feitas e recordes gravados (worker, como {@code app_worker}). */
public interface RecordHistory {

  /** Aluno e exercícios afetados por uma sessão (os dela e os que tinham recorde nela). */
  Optional<Scope> scopeOf(UUID sessionId);

  /** Séries concluídas de sessões finalizadas do aluno, nesses exercícios. */
  List<PersonalRecords.SetPerformance> finishedSets(UUID clientId, Collection<UUID> exerciseIds);

  /** Substitui todo o histórico de recordes do aluno nesses exercícios. */
  void replace(UUID clientId, Collection<UUID> exerciseIds, List<PersonalRecords.Record> records);

  record Scope(UUID clientId, Set<UUID> exerciseIds) {}
}
