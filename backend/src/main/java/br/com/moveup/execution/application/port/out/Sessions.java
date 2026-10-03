package br.com.moveup.execution.application.port.out;

import br.com.moveup.execution.domain.model.PerformedSession;
import br.com.moveup.execution.domain.model.SyncOutcome;
import java.util.Optional;
import java.util.UUID;

/** Sessões realizadas (com RLS: aluno grava as próprias; personal, as dos alunos dele). */
public interface Sessions {

  /** Trava a linha (FOR UPDATE) para dois envios iguais em paralelo não se cruzarem. */
  Optional<SyncOutcome.Stored> lockStored(UUID sessionId);

  /** Grava a sessão e substitui exercícios, séries, feedback e dores. */
  void write(
      PerformedSession session, UUID clientId, UUID performedByUser, boolean editedAfterFinish);

  /** Aviso para o worker ({@code session.finished}, {@code session.edited}): só ids. */
  void announce(UUID sessionId, UUID clientId, String eventType);
}
