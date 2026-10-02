package br.com.moveup.shared.domain;

/**
 * Regra violada pelo <em>estado atual</em> (já existe, já está ativo...): vira 409 em vez de 422.
 */
public abstract class ConflictException extends DomainException {

  protected ConflictException(String code, String safeMessage) {
    super(code, safeMessage);
  }
}
