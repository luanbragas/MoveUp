package br.com.moveup.coaching.domain.model;

import java.util.Locale;
import java.util.Optional;
import java.util.function.IntUnaryOperator;
import java.util.regex.Pattern;

/**
 * Código do convite: 8 caracteres sem os que se confundem ao ditar ou ler (0/O, 1/I/L). 31^8 ≈ 8,5
 * × 10^11 combinações, uso único e validade curta (ARQUITETURA 7.5).
 */
public record InviteCode(String value) {

  public static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
  public static final int LENGTH = 8;
  private static final Pattern FORMAT = Pattern.compile("[" + ALPHABET + "]{" + LENGTH + "}");

  public InviteCode {
    value = value == null ? "" : value.strip().toUpperCase(Locale.ROOT);
    if (!FORMAT.matcher(value).matches()) {
      throw new IllegalArgumentException("código de convite inválido");
    }
  }

  /**
   * @param randomIndex devolve um índice uniforme em [0, n) para o n dado (na infraestrutura, um
   *     {@code SecureRandom})
   */
  public static InviteCode generate(IntUnaryOperator randomIndex) {
    var code = new StringBuilder(LENGTH);
    for (int i = 0; i < LENGTH; i++) {
      code.append(ALPHABET.charAt(randomIndex.applyAsInt(ALPHABET.length())));
    }
    return new InviteCode(code.toString());
  }

  /** Código digitado pelo aluno: aceita minúsculas e espaços; inválido = vazio. */
  public static Optional<InviteCode> parse(String typed) {
    try {
      return Optional.of(new InviteCode(typed));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }
}
