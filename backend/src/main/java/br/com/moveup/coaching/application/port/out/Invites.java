package br.com.moveup.coaching.application.port.out;

import br.com.moveup.coaching.application.port.in.AnswerInvite.InvitePreview;
import br.com.moveup.coaching.domain.model.InviteCode;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Convites e as funções do banco que o aluno usa (accept_invite, invite_preview...). */
public interface Invites {

  void issue(UUID linkId, InviteCode code, Instant expiresAt);

  /**
   * @return quantos convites pendentes foram cancelados
   */
  int revokePending(UUID linkId);

  /** Função invite_preview: vazio se inválido, usado, cancelado ou expirado. */
  Optional<InvitePreview> preview(InviteCode code);

  /** Função accept_invite (trava a assinatura e confere o limite do plano). */
  UUID accept(InviteCode code);

  /** Função end_link_as_client. */
  void endAsClient(UUID linkId);

  /** Gera um código novo (aleatoriedade criptográfica). */
  InviteCode newCode();
}
