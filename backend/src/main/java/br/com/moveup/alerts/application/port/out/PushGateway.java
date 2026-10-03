package br.com.moveup.alerts.application.port.out;

import java.util.List;
import java.util.Map;

/** Envio ao Expo Push (chamada externa: nunca dentro de transação). */
public interface PushGateway {

  /**
   * Um resultado por mensagem, na mesma ordem.
   *
   * @throws PushUnavailable sem resposta (não dá para saber se entregou)
   */
  List<Result> send(List<Message> messages);

  record Message(String token, String title, String body, Map<String, String> data) {}

  /**
   * @param deviceGone o token não vale mais (remover)
   * @param retryable recusado sem entregar por limite ou erro temporário do Expo
   */
  record Result(boolean ok, boolean deviceGone, boolean retryable, String error) {}

  class PushUnavailable extends RuntimeException {
    public PushUnavailable(String reason) {
      super(reason);
    }
  }
}
