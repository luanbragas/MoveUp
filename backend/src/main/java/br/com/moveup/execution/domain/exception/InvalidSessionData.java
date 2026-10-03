package br.com.moveup.execution.domain.exception;

import br.com.moveup.shared.domain.DomainException;

/** Sessão enviada pelo app fora da regra; o {@code code} diz o quê. Nunca inclui o dado. */
public final class InvalidSessionData extends DomainException {

  public InvalidSessionData(String code, String safeMessage) {
    super(code, safeMessage);
  }
}
