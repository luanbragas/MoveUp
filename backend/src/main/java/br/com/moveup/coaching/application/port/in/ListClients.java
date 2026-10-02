package br.com.moveup.coaching.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Alunos do profissional (pendentes, ativos e inativos), paginados por cursor. */
public interface ListClients {

  int MAX_LIMIT = 100;

  ClientPage handle(UUID professionalId, UUID cursor, int limit);

  /**
   * @param startedAt nulo enquanto pendente
   * @param pendingInvite convite válido ainda não aceito (só para pendentes); nulo se não há
   */
  record ClientItem(
      UUID linkId,
      UUID clientId,
      String name,
      String status,
      Instant startedAt,
      PendingInvite pendingInvite) {}

  record PendingInvite(String code, Instant expiresAt) {}

  /**
   * @param nextCursor nulo quando não há mais páginas
   */
  record ClientPage(List<ClientItem> items, UUID nextCursor) {}
}
