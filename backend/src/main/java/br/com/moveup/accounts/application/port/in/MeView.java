package br.com.moveup.accounts.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Conta do usuário logado e o que falta no onboarding: o app decide a próxima tela por aqui.
 *
 * @param role {@code professional}, {@code client} ou nulo (cadastro sem papel)
 * @param missingConsents tipos ainda não aceitos na versão vigente
 * @param guardianConsentRequired menor sem consentimento autorizado pelo responsável
 * @param guardianRequest pedido ao responsável aguardando ou recusado; nulo se não há
 */
public record MeView(
    UUID id,
    String name,
    String email,
    String locale,
    String timezone,
    String weightUnit,
    String lengthUnit,
    String role,
    boolean minor,
    List<String> missingConsents,
    boolean guardianConsentRequired,
    GuardianRequestView guardianRequest) {

  /**
   * @param status {@code pending} (aguardando o responsável) ou {@code declined}
   * @param linkExpiresAt validade do último link; nulo se recusado
   */
  public record GuardianRequestView(
      String status,
      String guardianName,
      String relationship,
      Instant requestedAt,
      Instant linkExpiresAt) {}
}
