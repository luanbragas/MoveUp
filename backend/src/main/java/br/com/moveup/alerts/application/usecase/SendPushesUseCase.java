package br.com.moveup.alerts.application.usecase;

import br.com.moveup.alerts.application.port.out.PushDevices;
import br.com.moveup.alerts.application.port.out.PushGateway;
import br.com.moveup.alerts.application.port.out.PushQueue;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Envia a fila de push (laço do worker). Três passos: (1) numa transação, marca o lote como {@code
 * sending} e confirma; (2) fora de transação, chama o Expo; (3) noutra transação, grava o
 * resultado. Se o worker cair entre 1 e 3, a mensagem fica presa em {@code sending} e vira {@code
 * failed} depois: no máximo uma entrega, nunca duas (texto neutro: perder um aviso é melhor que
 * repetir).
 */
public class SendPushesUseCase {

  private static final int MAX_ATTEMPTS = 5;

  private final PushQueue queue;
  private final PushDevices devices;
  private final PushGateway gateway;
  private final TransactionTemplate tx;
  private final Clock clock;
  private final int batch;
  private final Duration staleAfter;

  public SendPushesUseCase(
      PushQueue queue,
      PushDevices devices,
      PushGateway gateway,
      TransactionTemplate tx,
      Clock clock,
      int batch,
      Duration staleAfter) {
    this.queue = queue;
    this.devices = devices;
    this.gateway = gateway;
    this.tx = tx;
    this.clock = clock;
    this.batch = batch;
    this.staleAfter = staleAfter;
  }

  /** Uma rodada; devolve quantas mensagens pegou (0 = fila vazia). */
  public int sendBatch() {
    var claimed =
        tx.execute(
            status -> {
              queue.failStale(clock.instant().minus(staleAfter));
              return queue.claim(batch, clock.instant());
            });
    if (claimed == null || claimed.isEmpty()) {
      return 0;
    }
    for (var push : claimed) {
      var tokens = tx.execute(status -> devices.tokensOf(push.userId()));
      if (tokens == null || tokens.isEmpty()) {
        tx.executeWithoutResult(status -> queue.markFailed(push.id(), "no-device"));
        continue;
      }
      var messages =
          tokens.stream()
              .map(t -> new PushGateway.Message(t, push.title(), push.body(), push.data()))
              .toList();
      List<PushGateway.Result> results;
      try {
        results = gateway.send(messages);
      } catch (PushGateway.PushUnavailable e) {
        // sem resposta: pode ter entregado; não reenvia
        tx.executeWithoutResult(status -> queue.markFailed(push.id(), "no-response"));
        continue;
      }
      record(push, tokens, results);
    }
    return claimed.size();
  }

  private void record(
      PushQueue.PendingPush push, List<String> tokens, List<PushGateway.Result> results) {
    var gone = new ArrayList<String>();
    var delivered = false;
    var retryable = false;
    String error = null;
    for (var i = 0; i < results.size() && i < tokens.size(); i++) {
      var r = results.get(i);
      delivered |= r.ok();
      retryable |= r.retryable();
      if (r.deviceGone()) {
        gone.add(tokens.get(i));
      }
      if (!r.ok() && error == null) {
        error = r.error();
      }
    }
    var attempts = push.attempts() + 1;
    var lastError = error == null ? "rejected" : error;
    var retry = !delivered && retryable && attempts < MAX_ATTEMPTS;
    var done = delivered;
    tx.executeWithoutResult(
        status -> {
          if (!gone.isEmpty()) {
            devices.removeTokens(gone);
          }
          if (done) {
            queue.markSent(push.id(), clock.instant());
          } else if (retry) {
            queue.markRetry(
                push.id(),
                attempts,
                clock.instant().plus(Duration.ofSeconds(30L << attempts)),
                lastError);
          } else {
            queue.markFailed(push.id(), lastError);
          }
        });
  }
}
