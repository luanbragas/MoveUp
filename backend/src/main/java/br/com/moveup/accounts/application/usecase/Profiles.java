package br.com.moveup.accounts.application.usecase;

import br.com.moveup.accounts.application.port.out.AccountReader.AccountSummary;
import br.com.moveup.accounts.domain.model.AccountProfile;
import br.com.moveup.accounts.domain.model.AccountRole;
import br.com.moveup.accounts.domain.model.BirthDate;
import br.com.moveup.accounts.domain.model.Email;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/** Apoio dos casos de uso: dia de hoje no Brasil e montagem do perfil a partir da leitura. */
final class Profiles {

  /** A maioridade conta pelo calendário do Brasil (o app é nacional no MVP). */
  static final ZoneId BRAZIL = ZoneId.of("America/Sao_Paulo");

  private Profiles() {}

  static LocalDate today(Clock clock) {
    return LocalDate.now(clock.withZone(BRAZIL));
  }

  static AccountProfile of(AccountSummary summary) {
    return new AccountProfile(
        summary.id(),
        summary.role() == null ? null : AccountRole.fromCode(summary.role()),
        Email.of(summary.email()),
        summary.birthDate() == null ? null : new BirthDate(summary.birthDate()));
  }
}
