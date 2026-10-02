package br.com.moveup.accounts.application.port.in;

import java.util.List;
import java.util.UUID;

/** Aceite e revogação dos textos legais, sempre na versão vigente (ARQUITETURA 7.8). */
public interface ManageConsents {

  void grant(UUID userId, List<Grant> grants, RequestOrigin origin);

  void revoke(UUID userId, String kind, RequestOrigin origin);

  LegalVersionsView currentVersions();

  record Grant(String kind, String docVersion) {}

  /** Versão vigente por tipo ({@code terms}, {@code privacy}...) e do termo do responsável. */
  record LegalVersionsView(java.util.Map<String, String> consents, String guardianConsent) {}
}
