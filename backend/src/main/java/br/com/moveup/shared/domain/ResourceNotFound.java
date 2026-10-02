package br.com.moveup.shared.domain;

/**
 * Recurso inexistente <em>ou</em> de outro usuário: os dois casos respondem 404 igual, para não
 * revelar que o recurso existe.
 */
public final class ResourceNotFound extends DomainException {

  public static final String CODE = "resource-not-found";

  public ResourceNotFound() {
    super(CODE, "Recurso não encontrado.");
  }
}
