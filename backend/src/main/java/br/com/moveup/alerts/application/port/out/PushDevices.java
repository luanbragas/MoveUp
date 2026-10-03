package br.com.moveup.alerts.application.port.out;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Tokens do Expo Push por usuário (um por aparelho). */
public interface PushDevices {

  /** O token muda de dono se outro usuário entrar no mesmo aparelho. */
  void register(UUID userId, String token, String platform, Instant now);

  void unregister(UUID userId, String token);

  List<String> tokensOf(UUID userId);

  /** Aparelho desinstalou o app ou trocou de token (o Expo avisa). */
  void removeTokens(Collection<String> tokens);
}
