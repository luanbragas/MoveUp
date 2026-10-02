package br.com.moveup.accounts.domain.exception;

import br.com.moveup.shared.domain.ResourceNotFound;

/**
 * Login válido no provedor, mas sem cadastro no MoveUp (ou cadastro excluído). O app usa este
 * {@code code} para levar ao cadastro.
 */
public final class AccountNotRegistered extends ResourceNotFound {

  public static final String CODE = "account-not-registered";

  public AccountNotRegistered() {
    super(CODE, "Conta não cadastrada.");
  }
}
