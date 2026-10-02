package br.com.moveup.accounts.application.port.out;

import br.com.moveup.accounts.application.port.in.RequestOrigin;
import br.com.moveup.accounts.domain.model.ConsentGrant;
import br.com.moveup.accounts.domain.model.ConsentKind;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Trilha de consentimento: cada aceite e cada revogação é uma linha (nunca apagada). */
public interface ConsentRepository {

  /** Versão do aceite mais recente não revogado de cada tipo. */
  Map<ConsentKind, String> acceptedVersions(UUID userId);

  void grant(UUID userId, List<ConsentGrant> grants, RequestOrigin origin);

  /**
   * @return se havia aceite vigente para revogar
   */
  boolean revoke(UUID userId, ConsentKind kind, RequestOrigin origin);
}
