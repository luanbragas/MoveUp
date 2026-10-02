package br.com.moveup.accounts.infrastructure.config;

import br.com.moveup.accounts.application.port.out.LegalDocuments;
import br.com.moveup.accounts.domain.model.ConsentKind;
import br.com.moveup.accounts.domain.model.LegalVersions;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Versões vigentes dos textos legais ({@code moveup.legal.versions.*}). Publicar texto novo =
 * trocar a versão aqui; quem aceitou a anterior volta a ver o aceite pendente no onboarding.
 */
@ConfigurationProperties("moveup.legal.versions")
record LegalVersionsProperties(
    String terms, String privacy, String healthData, String photos, String guardianConsent)
    implements LegalDocuments {

  @Override
  public LegalVersions current() {
    return new LegalVersions(
        Map.of(
            ConsentKind.TERMS, terms,
            ConsentKind.PRIVACY, privacy,
            ConsentKind.HEALTH_DATA, healthData,
            ConsentKind.PHOTOS, photos),
        guardianConsent);
  }
}
