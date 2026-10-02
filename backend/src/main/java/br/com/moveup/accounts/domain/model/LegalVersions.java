package br.com.moveup.accounts.domain.model;

import java.util.EnumSet;
import java.util.Map;

/**
 * Versões vigentes dos textos legais. Aceite de versão antiga não vale: quando o texto muda, o
 * usuário aceita de novo.
 */
public record LegalVersions(Map<ConsentKind, String> consents, String guardianConsent) {

  public LegalVersions {
    if (consents == null
        || !consents.keySet().containsAll(EnumSet.allOf(ConsentKind.class))
        || consents.values().stream().anyMatch(v -> v == null || v.isBlank())) {
      throw new IllegalArgumentException("versão vigente faltando para algum consentimento");
    }
    if (guardianConsent == null || guardianConsent.isBlank()) {
      throw new IllegalArgumentException("versão vigente do termo do responsável faltando");
    }
    consents = Map.copyOf(consents);
  }

  public String current(ConsentKind kind) {
    return consents.get(kind);
  }
}
