package br.com.moveup.coaching.application.port.out;

import br.com.moveup.coaching.application.port.in.ListClients.ClientPage;
import br.com.moveup.coaching.domain.model.ClientPreRegistration;
import br.com.moveup.coaching.domain.model.CoachingLink;
import java.util.Optional;
import java.util.UUID;

/** Vínculos e pré-cadastros (tabelas client e coaching_link, do módulo coaching). */
public interface CoachingLinks {

  /** Só o que o RLS deixa o usuário da transação ver. */
  Optional<CoachingLink> find(UUID linkId);

  void createPending(
      UUID clientId,
      UUID linkId,
      UUID organizationId,
      UUID professionalId,
      ClientPreRegistration client);

  void saveStatus(CoachingLink link);

  int countActive(UUID organizationId);

  ClientPage list(UUID professionalId, UUID cursor, int limit);
}
