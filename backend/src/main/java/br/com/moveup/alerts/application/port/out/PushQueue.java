package br.com.moveup.alerts.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Fila de push. Entra na transação do alerta; o worker pega ({@code sending}, confirmado antes da
 * chamada ao Expo), envia fora de transação e marca o resultado.
 */
public interface PushQueue {

  /** Idempotente pela chave (ex.: {@code alert:<id>}). */
  void enqueue(UUID userId, String dedupeKey, String title, String body, Map<String, String> data);

  /** Marca até {@code max} pendentes vencidos como {@code sending} e os devolve. */
  List<PendingPush> claim(int max, Instant now);

  void markSent(UUID pushId, Instant now);

  /** O Expo recusou sem entregar: tenta de novo depois. */
  void markRetry(UUID pushId, int attempts, Instant nextAttemptAt, String error);

  /** Não reenvia (sem aparelho, recusa definitiva ou resposta perdida). */
  void markFailed(UUID pushId, String error);

  /** Presos em {@code sending} desde antes de {@code before} (worker caiu): falham, sem reenvio. */
  int failStale(Instant before);

  record PendingPush(
      UUID id, UUID userId, String title, String body, Map<String, String> data, int attempts) {}
}
