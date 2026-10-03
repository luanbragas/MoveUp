package br.com.moveup.training.domain.exception;

import br.com.moveup.shared.domain.DomainException;

/** Dado de exercício, treino ou programa fora da regra; o {@code code} diz qual. */
public final class InvalidTrainingData extends DomainException {

  public InvalidTrainingData(String code, String safeMessage) {
    super(code, safeMessage);
  }
}
