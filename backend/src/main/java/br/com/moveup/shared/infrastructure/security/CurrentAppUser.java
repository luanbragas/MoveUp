package br.com.moveup.shared.infrastructure.security;

import java.util.Optional;
import java.util.UUID;

/**
 * Id interno ({@code app_user.id}) de quem faz a requisição atual. Vazio quando não há usuário
 * autenticado (requisição anônima, worker): nesse caso o RLS devolve zero linhas para {@code
 * app_api}.
 */
@FunctionalInterface
public interface CurrentAppUser {

  Optional<UUID> id();
}
