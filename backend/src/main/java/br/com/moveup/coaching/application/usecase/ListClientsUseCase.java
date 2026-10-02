package br.com.moveup.coaching.application.usecase;

import br.com.moveup.coaching.application.port.in.ListClients;
import br.com.moveup.coaching.application.port.out.CoachingLinks;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class ListClientsUseCase implements ListClients {

  private static final int DEFAULT_LIMIT = 50;

  private final CoachingLinks links;

  public ListClientsUseCase(CoachingLinks links) {
    this.links = links;
  }

  @Override
  @Transactional(readOnly = true)
  public ClientPage handle(UUID professionalId, UUID cursor, int limit) {
    var pageSize = limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
    return links.list(professionalId, cursor, pageSize);
  }
}
