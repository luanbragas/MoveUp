package br.com.moveup.accounts.domain.model;

import br.com.moveup.accounts.domain.exception.InvalidAccountData;
import java.util.Optional;

/** Dados do onboarding do profissional (SCREEN-FLOWS 0.2): nome do negócio e CREF opcional. */
public final class ProfessionalSetup {

  private static final int MAX_BUSINESS_NAME = 120;
  private static final int MAX_REGISTRY_NUMBER = 30;

  private final String businessName;
  private final String registryNumber; // nulo = não informado

  private ProfessionalSetup(String businessName, String registryNumber) {
    this.businessName = businessName;
    this.registryNumber = registryNumber;
  }

  /**
   * @param businessName vazio = usa o nome do profissional
   * @param registryNumber CREF; vazio = não informado
   */
  public static ProfessionalSetup of(String businessName, String registryNumber, PersonName owner) {
    var business = blankToNull(businessName);
    if (business == null) {
      business = owner.value();
    }
    if (business.length() < 2 || business.length() > MAX_BUSINESS_NAME) {
      throw new InvalidAccountData(
          "business-name-invalid", "Informe um nome de negócio entre 2 e 120 caracteres.");
    }
    var registry = blankToNull(registryNumber);
    if (registry != null && registry.length() > MAX_REGISTRY_NUMBER) {
      throw new InvalidAccountData("registry-number-invalid", "Número do CREF inválido.");
    }
    return new ProfessionalSetup(business, registry);
  }

  public String businessName() {
    return businessName;
  }

  public Optional<String> registryNumber() {
    return Optional.ofNullable(registryNumber);
  }

  private static String blankToNull(String value) {
    if (value == null) {
      return null;
    }
    var stripped = value.strip();
    return stripped.isEmpty() ? null : stripped;
  }
}
