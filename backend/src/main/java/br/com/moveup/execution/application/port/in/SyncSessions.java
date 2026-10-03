package br.com.moveup.execution.application.port.in;

import br.com.moveup.execution.domain.model.PerformedSession;
import java.util.List;
import java.util.UUID;

/**
 * Envio do sync (ARQUITETURA 4): sessões registradas offline pelo aluno, ou pelo personal no modo
 * presencial. Idempotente: reenviar o mesmo lote não duplica nem muda nada.
 */
public interface SyncSessions {

  int MAX_SESSIONS = 50;

  SyncResult push(UUID userId, List<PerformedSession> sessions);

  /**
   * @param written gravadas agora (novas ou edição mais recente)
   * @param unchanged já estavam iguais ou mais novas no servidor (o app pode marcar como enviadas)
   */
  record SyncResult(List<UUID> written, List<UUID> unchanged) {}
}
