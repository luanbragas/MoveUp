package br.com.moveup.accounts.domain.exception;

import br.com.moveup.shared.domain.ConflictException;

/** O app mostrou uma versão antiga do texto: o aceite precisa ser da versão vigente. */
public final class ConsentVersionOutdated extends ConflictException {

  public static final String CODE = "consent-version-outdated";

  public ConsentVersionOutdated() {
    super(CODE, "O texto foi atualizado. Leia a versão nova para continuar.");
  }
}
