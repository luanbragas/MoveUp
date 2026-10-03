package br.com.moveup.accounts.domain.exception;

import br.com.moveup.shared.domain.ResourceNotFound;

/**
 * Link do responsável que não serve mais: inexistente, vencido, já usado ou cancelado pelo menor.
 * Os casos respondem igual para não revelar se o pedido existe.
 */
public final class GuardianAuthorizationNotFound extends ResourceNotFound {

  public static final String CODE = "guardian-authorization-not-found";

  public GuardianAuthorizationNotFound() {
    super(CODE, "Este link de autorização não vale mais.");
  }
}
