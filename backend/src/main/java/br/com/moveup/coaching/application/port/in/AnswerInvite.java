package br.com.moveup.coaching.application.port.in;

import java.time.Instant;
import java.util.UUID;

/** O aluno vê de quem é o convite e aceita (SCREEN-FLOWS 1.2). */
public interface AnswerInvite {

  InvitePreview preview(String code);

  /**
   * @return o vínculo, agora ativo
   */
  UUID accept(UUID userId, String code);

  record InvitePreview(String professionalName, String organizationName, Instant expiresAt) {}
}
