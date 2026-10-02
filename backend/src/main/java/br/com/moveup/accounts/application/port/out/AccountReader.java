package br.com.moveup.accounts.application.port.out;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/** Leitura de contas ativas (não anonimizadas). */
public interface AccountReader {

  Optional<AccountSummary> find(UUID userId);

  /**
   * @param role nulo se a conta não escolheu papel; birthDate nula se não informada
   */
  record AccountSummary(
      UUID id,
      String name,
      String email,
      String locale,
      String timezone,
      String weightUnit,
      String lengthUnit,
      String role,
      LocalDate birthDate) {}
}
