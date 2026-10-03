package br.com.moveup.execution.application.usecase;

import br.com.moveup.execution.application.port.out.RecordHistory;
import br.com.moveup.execution.domain.model.PersonalRecords;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recordes depois que uma sessão termina ou é corrigida: refaz o histórico dos exercícios dela a
 * partir de todas as séries do aluno. Idempotente (o worker pode repetir o evento sem efeito).
 */
public class RecalculateRecordsUseCase {

  private final RecordHistory history;

  public RecalculateRecordsUseCase(RecordHistory history) {
    this.history = history;
  }

  @Transactional
  public void forSession(UUID sessionId) {
    history
        .scopeOf(sessionId)
        .filter(scope -> !scope.exerciseIds().isEmpty())
        .ifPresent(
            scope ->
                history.replace(
                    scope.clientId(),
                    scope.exerciseIds(),
                    PersonalRecords.history(
                        history.finishedSets(scope.clientId(), scope.exerciseIds()))));
  }
}
