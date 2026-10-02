package br.com.moveup.shared.domain;

/**
 * Regra de negócio violada. Carrega só um {@code code} estável (kebab-case, usado pelo app para
 * escolher a mensagem) e uma mensagem segura: nunca dado de saúde, nome ou e-mail.
 *
 * <p>Vira 422 no {@code GlobalProblemHandler}, a não ser que a subclasse seja mapeada para outro
 * status.
 */
public abstract class DomainException extends RuntimeException {

  private final String code;

  protected DomainException(String code, String safeMessage) {
    super(safeMessage);
    if (code == null || !code.matches("[a-z0-9]+(-[a-z0-9]+)*")) {
      throw new IllegalArgumentException("code deve ser kebab-case: " + code);
    }
    this.code = code;
  }

  public String code() {
    return code;
  }
}
