package br.com.moveup.coaching.application.usecase;

import br.com.moveup.accounts.api.AccountDirectory;
import br.com.moveup.billing.api.PlanLimits;
import br.com.moveup.coaching.application.port.in.ListClients;
import br.com.moveup.coaching.application.port.out.CoachingLinks;
import br.com.moveup.shared.domain.Forbidden;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class ListClientsUseCase implements ListClients {

  private static final int DEFAULT_LIMIT = 50;

  private final CoachingLinks links;
  private final AccountDirectory accounts;
  private final PlanLimits planLimits;

  public ListClientsUseCase(CoachingLinks links, AccountDirectory accounts, PlanLimits planLimits) {
    this.links = links;
    this.accounts = accounts;
    this.planLimits = planLimits;
  }

  @Override
  @Transactional(readOnly = true)
  public ClientPage handle(UUID professionalId, UUID cursor, int limit) {
    var pageSize = limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
    return links.list(professionalId, cursor, pageSize);
  }

  @Override
  @Transactional(readOnly = true)
  public Seats seats(UUID professionalId) {
    var organizationId = accounts.organizationOf(professionalId).orElseThrow(Forbidden::new);
    var limit = planLimits.activeClientLimit(organizationId);
    return new Seats(
        links.countActive(organizationId), limit.isPresent() ? limit.getAsInt() : null);
  }
}
