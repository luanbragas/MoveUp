package br.com.moveup.coaching.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Vínculos do próprio aluno (não encerrados): a casa do aluno mostra com quem ele está. */
public interface MyCoachingLinks {

  List<MyLink> handle(UUID userId);

  /**
   * @param startedAt nulo enquanto pendente
   */
  record MyLink(
      UUID linkId,
      String status,
      Instant startedAt,
      String professionalName,
      String organizationName) {}
}
