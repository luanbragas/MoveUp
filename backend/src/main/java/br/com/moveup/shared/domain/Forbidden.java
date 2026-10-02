package br.com.moveup.shared.domain;

/**
 * O usuário existe e está autenticado, mas o papel dele não permite a ação (ex.: aluno tentando
 * convidar). Vira 403 {@code forbidden}. Recurso de outro usuário continua sendo {@link
 * ResourceNotFound} (404), para não revelar que existe.
 */
public final class Forbidden extends DomainException {

  public static final String CODE = "forbidden";

  public Forbidden() {
    super(CODE, "Você não tem acesso a isso.");
  }
}
