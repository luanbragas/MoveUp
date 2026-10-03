package br.com.moveup.accounts.application.port.in;

import java.time.Instant;

/**
 * Lado do responsável, que não tem conta: abre o link, vê o pedido e autoriza ou recusa. Link
 * inválido, vencido ou já usado responde {@code guardian-authorization-not-found}.
 */
public interface GuardianAuthorization {

  Preview preview(String token);

  void decide(Decision decision);

  /**
   * O mínimo para o responsável decidir: só o primeiro nome do menor, nada de dado de saúde.
   *
   * @param docVersion versão do termo que a página mostra e o responsável aceita
   */
  record Preview(
      String minorFirstName,
      String guardianName,
      String relationship,
      String docVersion,
      Instant expiresAt) {}

  record Decision(String token, boolean approve, String docVersion, RequestOrigin origin) {}
}
