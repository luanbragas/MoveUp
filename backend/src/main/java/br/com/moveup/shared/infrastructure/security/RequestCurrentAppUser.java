package br.com.moveup.shared.infrastructure.security;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Usuário da requisição HTTP atual, resolvido uma vez por {@link CurrentAppUserFilter}. Fora de
 * requisição (worker, jobs) é sempre vazio.
 */
@Component
public class RequestCurrentAppUser implements CurrentAppUser {

  static final String ATTRIBUTE = RequestCurrentAppUser.class.getName() + ".id";

  @Override
  public Optional<UUID> id() {
    var attributes = RequestContextHolder.getRequestAttributes();
    if (attributes == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(
        (UUID) attributes.getAttribute(ATTRIBUTE, RequestAttributes.SCOPE_REQUEST));
  }
}
