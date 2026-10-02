package br.com.moveup.accounts.domain.exception;

import br.com.moveup.shared.domain.ConflictException;

/** Este login (ou este e-mail) já tem conta no MoveUp. */
public final class AccountAlreadyRegistered extends ConflictException {

  public static final String CODE = "account-already-registered";

  public AccountAlreadyRegistered() {
    super(CODE, "Esta conta já está cadastrada.");
  }
}
