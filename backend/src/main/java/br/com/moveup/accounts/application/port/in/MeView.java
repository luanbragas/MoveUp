package br.com.moveup.accounts.application.port.in;

import java.util.List;
import java.util.UUID;

/**
 * Conta do usuário logado e o que falta no onboarding: o app decide a próxima tela por aqui.
 *
 * @param role {@code professional}, {@code client} ou nulo (cadastro sem papel)
 * @param missingConsents tipos ainda não aceitos na versão vigente
 * @param guardianConsentRequired menor sem consentimento do responsável vigente
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
    boolean guardianConsentRequired) {}
