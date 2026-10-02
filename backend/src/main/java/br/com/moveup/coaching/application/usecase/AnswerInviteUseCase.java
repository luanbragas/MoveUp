package br.com.moveup.coaching.application.usecase;

import br.com.moveup.accounts.api.AccountDirectory;
import br.com.moveup.coaching.application.port.in.AnswerInvite;
import br.com.moveup.coaching.application.port.out.Invites;
import br.com.moveup.coaching.domain.exception.CoachingConflict;
import br.com.moveup.coaching.domain.model.InviteCode;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prévia e aceite do convite. Só conta de aluno com o onboarding completo (aceites e, se menor, o
 * responsável) aceita; vaga do plano, uso único e troca de personal ficam no accept_invite.
 */
public class AnswerInviteUseCase implements AnswerInvite {

  private final AccountDirectory accounts;
  private final Invites invites;

  public AnswerInviteUseCase(AccountDirectory accounts, Invites invites) {
    this.accounts = accounts;
    this.invites = invites;
  }

  @Override
  @Transactional(readOnly = true)
  public InvitePreview preview(String code) {
    return InviteCode.parse(code)
        .flatMap(invites::preview)
        .orElseThrow(CoachingConflict::inviteExpired);
  }

  @Override
  @Transactional
  public UUID accept(UUID userId, String code) {
    var inviteCode = InviteCode.parse(code).orElseThrow(CoachingConflict::inviteExpired);
    switch (accounts.linkReadiness(userId)) {
      case NOT_A_CLIENT -> throw CoachingConflict.inviteForClientsOnly();
      case ONBOARDING_INCOMPLETE -> throw CoachingConflict.onboardingIncomplete();
      case READY -> {
        // segue para o aceite
      }
    }
    return invites.accept(inviteCode);
  }
}
