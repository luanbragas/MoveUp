package br.com.moveup.accounts.domain.model;

import br.com.moveup.accounts.domain.exception.ConsentVersionOutdated;

/** Aceite de um texto legal, sempre da versão vigente. */
public record ConsentGrant(ConsentKind kind, String docVersion) {

  public static ConsentGrant accept(ConsentKind kind, String docVersion, LegalVersions current) {
    if (docVersion == null || !docVersion.equals(current.current(kind))) {
      throw new ConsentVersionOutdated();
    }
    return new ConsentGrant(kind, docVersion);
  }
}
