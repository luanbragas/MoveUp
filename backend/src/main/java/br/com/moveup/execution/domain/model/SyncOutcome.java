package br.com.moveup.execution.domain.model;

import java.time.Instant;

/**
 * O que fazer com uma sessão que chegou pelo sync, comparando com a gravada. Vence a edição mais
 * recente no aparelho; reenviar a mesma não muda nada.
 *
 * @param write gravar (sessão nova ou edição mais recente)
 * @param editedAfterFinish já estava finalizada e foi editada depois
 * @param justFinished passou a finalizada agora (aviso para o worker: recordes, alertas)
 */
public record SyncOutcome(boolean write, boolean editedAfterFinish, boolean justFinished) {

  /** O que já está gravado da sessão (só o que decide). */
  public record Stored(PerformedSession.Status status, Instant clientUpdatedAt) {}

  public static SyncOutcome decide(PerformedSession incoming, Stored stored) {
    if (stored == null) {
      return new SyncOutcome(true, false, incoming.status().finished());
    }
    if (!incoming.clientUpdatedAt().isAfter(stored.clientUpdatedAt())) {
      return new SyncOutcome(false, false, false); // reenvio ou edição mais antiga
    }
    var wasFinished = stored.status().finished();
    return new SyncOutcome(
        true,
        wasFinished && incoming.status().finished(),
        !wasFinished && incoming.status().finished());
  }
}
