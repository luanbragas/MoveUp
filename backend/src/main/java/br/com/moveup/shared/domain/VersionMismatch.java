package br.com.moveup.shared.domain;

/**
 * Edição baseada numa versão antiga do recurso ({@code If-Match} desatualizado): outra pessoa ou
 * outro aparelho salvou antes. Vira 412; sem {@code If-Match}, 428 ({@code if-match-required}).
 */
public final class VersionMismatch extends DomainException {

  public static final String CODE = "version-mismatch";
  public static final String REQUIRED = "if-match-required";

  private final boolean missing;

  private VersionMismatch(String code, String message, boolean missing) {
    super(code, message);
    this.missing = missing;
  }

  public static VersionMismatch stale() {
    return new VersionMismatch(CODE, "O recurso foi alterado em outro aparelho.", false);
  }

  public static VersionMismatch required() {
    return new VersionMismatch(
        REQUIRED, "Envie o cabeçalho If-Match com a versão que você editou.", true);
  }

  public boolean missing() {
    return missing;
  }
}
