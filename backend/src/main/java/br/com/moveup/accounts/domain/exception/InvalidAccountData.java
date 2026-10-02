package br.com.moveup.accounts.domain.exception;

import br.com.moveup.shared.domain.DomainException;

/** Dado de cadastro inválido. O {@code code} diz qual campo (ex.: {@code birth-date-invalid}). */
public final class InvalidAccountData extends DomainException {

  public InvalidAccountData(String code, String safeMessage) {
    super(code, safeMessage);
  }
}
