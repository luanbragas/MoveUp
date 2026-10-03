package br.com.moveup.coaching.api;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Para o módulo alerts: de quem é cada vínculo (worker), os vínculos ativos (jobs diários) e o nome
 * dos alunos para a central de atenção (API, com RLS do profissional).
 */
public interface CoachingRoster {

  Optional<LinkOwner> owner(UUID linkId);

  /** Todos os vínculos ativos (worker). */
  List<LinkOwner> activeLinks();

  /** Nome e vínculo mais recente de cada aluno com este profissional. */
  Map<UUID, ClientRef> clientsOf(UUID professionalId, Collection<UUID> clientIds);

  /** O cadastro de aluno desta conta e o vínculo ativo (nulo sem vínculo ativo). */
  Optional<ClientSelf> clientOfUser(UUID userId);

  record ClientSelf(UUID clientId, UUID activeLinkId) {}

  record LinkOwner(
      UUID linkId,
      UUID clientId,
      UUID professionalId,
      UUID organizationId,
      String status,
      Instant startedAt) {}

  record ClientRef(UUID clientId, UUID linkId, String name) {}
}
