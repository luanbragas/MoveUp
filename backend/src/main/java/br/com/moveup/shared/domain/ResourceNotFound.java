package br.com.moveup.shared.domain;

/**
 * Recurso inexistente <em>ou</em> de outro usuário: os dois casos respondem 404 igual, para não
 * revelar que o recurso existe. Subclasses só quando o app precisa distinguir o caso (ex.: {@code
 * account-not-registered}, que leva ao cadastro).
 */
public class ResourceNotFound extends DomainException {

  public static final String CODE = "resource-not-found";

  public ResourceNotFound() {
    this(CODE, "Recurso não encontrado.");
  }

  protected ResourceNotFound(String code, String safeMessage) {
    super(code, safeMessage);
  }
}
