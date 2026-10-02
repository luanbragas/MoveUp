package br.com.moveup.accounts.domain.model;

import br.com.moveup.accounts.domain.exception.InvalidAccountData;

/** Nome de pessoa, sem espaços sobrando, entre 2 e 200 caracteres. */
public record PersonName(String value) {

  public PersonName {
    value = value == null ? "" : value.strip().replaceAll("\\s+", " ");
    if (value.length() < 2 || value.length() > 200) {
      throw new InvalidAccountData("name-invalid", "Informe um nome entre 2 e 200 caracteres.");
    }
  }

  public static PersonName of(String value) {
    return new PersonName(value);
  }
}
