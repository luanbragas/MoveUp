package br.com.moveup.coaching.domain.exception;

import br.com.moveup.shared.domain.DomainException;

/** Pré-cadastro do aluno com dado inválido; o {@code code} diz qual campo. */
public final class InvalidClientData extends DomainException {

  public InvalidClientData(String code, String safeMessage) {
    super(code, safeMessage);
  }
}
