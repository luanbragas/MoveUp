package br.com.moveup.coaching.domain.exception;

import br.com.moveup.shared.domain.ConflictException;

/** Regras do vínculo barradas pelo estado atual (409), cada uma com seu {@code code}. */
public final class CoachingConflict extends ConflictException {

  public static final String LINK_STATE_INVALID = "link-state-invalid";
  public static final String PLAN_LIMIT_REACHED = "plan-limit-reached";
  public static final String ONBOARDING_INCOMPLETE = "onboarding-incomplete";
  public static final String INVITE_FOR_CLIENTS_ONLY = "invite-for-clients-only";

  private CoachingConflict(String code, String safeMessage) {
    super(code, safeMessage);
  }

  /** Mesmo code que o banco devolve (P0002): o app trata igual inválido, usado ou expirado. */
  public static final String INVITE_EXPIRED = "invite-expired";

  public static CoachingConflict inviteExpired() {
    return new CoachingConflict(INVITE_EXPIRED, "Convite inválido ou expirado.");
  }

  public static CoachingConflict linkStateInvalid() {
    return new CoachingConflict(LINK_STATE_INVALID, "O vínculo não permite essa ação agora.");
  }

  public static CoachingConflict planLimitReached() {
    return new CoachingConflict(PLAN_LIMIT_REACHED, "Limite de alunos ativos do plano atingido.");
  }

  public static CoachingConflict onboardingIncomplete() {
    return new CoachingConflict(
        ONBOARDING_INCOMPLETE, "Termine o cadastro (termos e autorizações) antes de aceitar.");
  }

  public static CoachingConflict inviteForClientsOnly() {
    return new CoachingConflict(INVITE_FOR_CLIENTS_ONLY, "Convites são para contas de aluno.");
  }
}
