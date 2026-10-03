package br.com.moveup.anamnesis.domain.exception;

import br.com.moveup.shared.domain.DomainException;

/** Anamnese ou restrição fora da regra (422). A mensagem nunca repete a resposta do aluno. */
public class InvalidAnamnesisData extends DomainException {

  public InvalidAnamnesisData(String code, String safeMessage) {
    super(code, safeMessage);
  }
}
