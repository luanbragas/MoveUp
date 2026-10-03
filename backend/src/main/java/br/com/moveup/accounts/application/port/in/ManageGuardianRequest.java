package br.com.moveup.accounts.application.port.in;

import java.time.Instant;
import java.util.UUID;

/**
 * Lado do menor no consentimento do responsável (LGPD, art. 14): indica quem autoriza, recebe o
 * link para mandar a essa pessoa, gera um link novo ou desiste do pedido.
 */
public interface ManageGuardianRequest {

  GuardianLink request(RequestCommand command);

  /** Link novo para o mesmo pedido; o anterior deixa de valer. */
  GuardianLink resendLink(UUID userId);

  void cancel(UUID userId);

  record RequestCommand(
      UUID userId,
      String guardianName,
      String relationship,
      String docVersion,
      RequestOrigin origin) {}

  /** O segredo só existe aqui: o banco guarda o hash, então o link não pode ser lido de novo. */
  record GuardianLink(String url, Instant expiresAt) {}
}
