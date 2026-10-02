package br.com.moveup.accounts.application.port.out;

import br.com.moveup.accounts.domain.model.LegalVersions;

/** Versões vigentes dos textos legais (termos, privacidade, saúde, fotos, responsável). */
public interface LegalDocuments {

  LegalVersions current();
}
