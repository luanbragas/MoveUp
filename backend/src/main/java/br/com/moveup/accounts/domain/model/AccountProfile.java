package br.com.moveup.accounts.domain.model;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Conta já cadastrada, com o que decide o onboarding: papel, idade e aceites vigentes. Calcula o
 * que ainda falta para o app liberar o uso.
 */
public final class AccountProfile {

  private final UUID userId;
  private final AccountRole role; // nulo: conta antiga sem papel
  private final Email email;
  private final BirthDate birthDate; // nulo: não informada

  public AccountProfile(UUID userId, AccountRole role, Email email, BirthDate birthDate) {
    this.userId = userId;
    this.role = role;
    this.email = email;
    this.birthDate = birthDate;
  }

  public UUID userId() {
    return userId;
  }

  public Optional<AccountRole> role() {
    return Optional.ofNullable(role);
  }

  public Email email() {
    return email;
  }

  public boolean isMinorOn(LocalDate today) {
    return birthDate != null && birthDate.isMinorOn(today);
  }

  /**
   * @param accepted versão aceita (e não revogada) de cada consentimento
   * @param guardianConsentActive há consentimento autorizado pelo responsável e não revogado
   */
  public Onboarding onboardingOn(
      LocalDate today,
      Map<ConsentKind, String> accepted,
      boolean guardianConsentActive,
      LegalVersions current) {
    var required = role().map(AccountRole::requiredConsents).orElseGet(Set::of);
    var missing = EnumSet.noneOf(ConsentKind.class);
    for (var kind : required) {
      if (!current.current(kind).equals(accepted.get(kind))) {
        missing.add(kind);
      }
    }
    var minor = isMinorOn(today);
    return new Onboarding(missing, minor, minor && !guardianConsentActive);
  }

  /** O que falta: aceites pendentes e, para menor, o consentimento do responsável. */
  public record Onboarding(
      Set<ConsentKind> missingConsents, boolean minor, boolean guardianConsentRequired) {

    public Onboarding {
      missingConsents = Set.copyOf(missingConsents);
    }

    public boolean complete() {
      return missingConsents.isEmpty() && !guardianConsentRequired;
    }
  }
}
