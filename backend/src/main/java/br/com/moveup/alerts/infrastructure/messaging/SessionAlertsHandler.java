package br.com.moveup.alerts.infrastructure.messaging;

import br.com.moveup.alerts.application.usecase.SessionAlertsUseCase;
import br.com.moveup.shared.application.outbox.OutboxEvent;
import br.com.moveup.shared.application.outbox.OutboxHandler;
import java.util.Set;

/** Alertas a partir dos eventos de sessão publicados pelo execution. */
public class SessionAlertsHandler implements OutboxHandler {

  static final String SESSION_FINISHED = "session.finished";
  static final String SESSION_EDITED = "session.edited";

  private final SessionAlertsUseCase alerts;

  public SessionAlertsHandler(SessionAlertsUseCase alerts) {
    this.alerts = alerts;
  }

  @Override
  public Set<String> types() {
    return Set.of(SESSION_FINISHED, SESSION_EDITED);
  }

  @Override
  public void handle(OutboxEvent event) {
    if (SESSION_EDITED.equals(event.type())) {
      alerts.edited(event.aggregateId());
    } else {
      alerts.finished(event.aggregateId());
    }
  }
}
