package br.com.moveup.accounts.domain.model;

import br.com.moveup.accounts.domain.exception.InvalidAccountData;
import java.util.Locale;
import java.util.regex.Pattern;

/** E-mail normalizado (sem espaços, minúsculo). A comparação sem caixa também vale no banco. */
public record Email(String value) {

  // Verificação de forma, não de existência: quem garante o e-mail é o provedor de login.
  private static final Pattern FORMAT = Pattern.compile("^[^@\\s]{1,64}@[^@\\s]+\\.[^@\\s]{2,}$");
  private static final int MAX_LENGTH = 254;

  public Email {
    value = value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
    if (value.length() > MAX_LENGTH || !FORMAT.matcher(value).matches()) {
      throw new InvalidAccountData("email-invalid", "E-mail inválido.");
    }
  }

  public static Email of(String value) {
    return new Email(value);
  }
}
