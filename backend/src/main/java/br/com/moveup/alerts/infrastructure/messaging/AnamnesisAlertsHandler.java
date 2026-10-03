package br.com.moveup.alerts.infrastructure.messaging;

import br.com.moveup.alerts.application.usecase.ClearanceAlertsUseCase;
import br.com.moveup.shared.application.outbox.OutboxEvent;
import br.com.moveup.shared.application.outbox.OutboxHandler;
import java.util.Set;

/** Anamnese enviada ou revisada: abre ou resolve o alerta de liberação médica pendente. */
public class AnamnesisAlertsHandler implements OutboxHandler {

  private final ClearanceAlertsUseCase clearance;

  public AnamnesisAlertsHandler(ClearanceAlertsUseCase clearance) {
    this.clearance = clearance;
  }

  @Override
  public Set<String> types() {
    return Set.of("anamnesis.submitted", "anamnesis.reviewed");
  }

  @Override
  public void handle(OutboxEvent event) {
    clearance.changed(event.aggregateId());
  }
}
