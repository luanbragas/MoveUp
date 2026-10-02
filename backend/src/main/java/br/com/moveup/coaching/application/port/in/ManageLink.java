package br.com.moveup.coaching.application.port.in;

import br.com.moveup.coaching.application.port.in.InviteClient.Invitation;
import java.util.UUID;

/** Ações sobre um vínculo depois de criado: convite e ciclo de vida (SCREEN-FLOWS 2.2). */
public interface ManageLink {

  /** Cancela o convite pendente e gera outro (link anterior deixa de valer). */
  Invitation resendInvite(UUID professionalId, UUID linkId);

  /** Cancela o convite pendente; o aluno continua pré-cadastrado (dá para reenviar). */
  void cancelInvite(UUID professionalId, UUID linkId);

  void inactivate(UUID professionalId, UUID linkId);

  /** Exige vaga no plano (checada com a assinatura travada). */
  void reactivate(UUID professionalId, UUID linkId);

  void end(UUID professionalId, UUID linkId);

  /** O próprio aluno encerra o vínculo. */
  void endAsClient(UUID userId, UUID linkId);
}
