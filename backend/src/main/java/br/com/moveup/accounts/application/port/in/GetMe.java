package br.com.moveup.accounts.application.port.in;

import java.util.UUID;

/** Conta do usuário autenticado. */
public interface GetMe {

  MeView handle(UUID userId);
}
