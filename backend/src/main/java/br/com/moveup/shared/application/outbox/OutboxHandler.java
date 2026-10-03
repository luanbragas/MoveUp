package br.com.moveup.shared.application.outbox;

import java.util.Set;

/**
 * Reação de um módulo a um evento do outbox (ex.: {@code session.finished} → recordes, alertas).
 * Roda dentro da transação do evento, junto com a marcação de processado: ou tudo grava, ou nada (e
 * o evento volta para a fila). Por isso o handler precisa ser idempotente e não pode fazer chamada
 * externa (push, S3): efeito externo vira outra fila, gravada aqui.
 */
public interface OutboxHandler {

  /** Tipos de evento tratados (ex.: {@code session.finished}). */
  Set<String> types();

  void handle(OutboxEvent event);
}
