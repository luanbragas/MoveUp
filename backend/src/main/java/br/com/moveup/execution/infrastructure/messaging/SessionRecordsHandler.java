package br.com.moveup.execution.infrastructure.messaging;

import br.com.moveup.execution.application.usecase.RecalculateRecordsUseCase;
import br.com.moveup.execution.application.usecase.SyncSessionsUseCase;
import br.com.moveup.shared.application.outbox.OutboxEvent;
import br.com.moveup.shared.application.outbox.OutboxHandler;
import java.util.Set;

/** Sessão finalizada ou corrigida: refaz os recordes dos exercícios dela. */
public class SessionRecordsHandler implements OutboxHandler {

  private final RecalculateRecordsUseCase records;

  public SessionRecordsHandler(RecalculateRecordsUseCase records) {
    this.records = records;
  }

  @Override
  public Set<String> types() {
    return Set.of(SyncSessionsUseCase.SESSION_FINISHED, SyncSessionsUseCase.SESSION_EDITED);
  }

  @Override
  public void handle(OutboxEvent event) {
    records.forSession(event.aggregateId());
  }
}
