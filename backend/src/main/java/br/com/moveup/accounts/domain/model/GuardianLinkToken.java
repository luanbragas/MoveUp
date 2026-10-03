package br.com.moveup.accounts.domain.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Segredo do link que o responsável recebe: 32 bytes aleatórios em base64url (43 caracteres). Quem
 * tem o link decide pelo menor, então o banco guarda só o {@link #hash() SHA-256}.
 */
public record GuardianLinkToken(String value) {

  private static final int BYTES = 32;
  private static final Pattern FORMAT = Pattern.compile("[A-Za-z0-9_-]{43}");

  public GuardianLinkToken {
    if (value == null || !FORMAT.matcher(value).matches()) {
      throw new IllegalArgumentException("segredo do link inválido");
    }
  }

  /**
   * @param randomBytes preenche o vetor com bytes aleatórios (na infraestrutura, um {@code
   *     SecureRandom})
   */
  public static GuardianLinkToken generate(Consumer<byte[]> randomBytes) {
    var bytes = new byte[BYTES];
    randomBytes.accept(bytes);
    return new GuardianLinkToken(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
  }

  /** Segredo vindo da página do responsável; formato inválido = vazio (vira 404, como vencido). */
  public static Optional<GuardianLinkToken> parse(String raw) {
    try {
      return Optional.of(new GuardianLinkToken(raw));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }

  public byte[] hash() {
    try {
      return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.US_ASCII));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("JVM sem SHA-256", e);
    }
  }

  @Override
  public String toString() {
    return "GuardianLinkToken[***]"; // nunca em log
  }
}
