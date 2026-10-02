package br.com.moveup.accounts.domain.model;

import br.com.moveup.accounts.domain.exception.InvalidAccountData;
import java.time.LocalDate;
import java.time.Period;

/**
 * Data de nascimento: decide se a conta é de menor (consentimento do responsável, LGPD art. 14).
 */
public record BirthDate(LocalDate value) {

  public static final int ADULT_AGE = 18;
  private static final int MAX_AGE = 120;
  private static final int MIN_AGE = 5;

  public BirthDate {
    if (value == null) {
      throw new InvalidAccountData("birth-date-required", "Informe a data de nascimento.");
    }
  }

  /** Valida a data em relação a hoje: nem no futuro, nem absurda. */
  public static BirthDate of(LocalDate value, LocalDate today) {
    var birthDate = new BirthDate(value);
    var age = birthDate.ageOn(today);
    if (value.isAfter(today) || age < MIN_AGE || age > MAX_AGE) {
      throw new InvalidAccountData("birth-date-invalid", "Data de nascimento inválida.");
    }
    return birthDate;
  }

  public int ageOn(LocalDate day) {
    return Period.between(value, day).getYears();
  }

  public boolean isMinorOn(LocalDate day) {
    return ageOn(day) < ADULT_AGE;
  }
}
