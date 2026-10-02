package br.com.moveup.shared.infrastructure.security;

import java.util.Optional;
import java.util.UUID;

/**
 * Resolve a identidade do provedor de login ({@code provider}, {@code sub}) para o id interno do
 * usuário ({@code auth_identity} → {@code app_user}). Implementado pelo módulo {@code accounts},
 * que é dono dessas tabelas; o {@code shared} só conhece esta interface.
 */
@FunctionalInterface
public interface AppUserResolver {

  Optional<UUID> resolve(String provider, String subject);
}
