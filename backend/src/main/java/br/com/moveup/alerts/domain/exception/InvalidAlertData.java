package br.com.moveup.alerts.domain.exception;

import br.com.moveup.shared.domain.DomainException;

/** Configuração de alerta ou ação inválida (422). */
public class InvalidAlertData extends DomainException {

  public InvalidAlertData(String code, String safeMessage) {
    super(code, safeMessage);
  }
}
