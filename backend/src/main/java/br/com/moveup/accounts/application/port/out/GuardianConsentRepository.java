package br.com.moveup.accounts.application.port.out;

import br.com.moveup.accounts.application.port.in.RequestOrigin;
import br.com.moveup.accounts.domain.model.GuardianConsent;
import br.com.moveup.accounts.domain.model.GuardianLinkToken;
import java.util.Optional;
import java.util.UUID;

/**
 * Pedidos de consentimento do responsável. As leituras e gravações do menor passam pelo RLS (só o
 * próprio usuário); as do responsável, que não tem conta, só pelo segredo do link.
 */
public interface GuardianConsentRepository {

  /** Consentimento autorizado pelo responsável e não revogado. */
  boolean hasVerified(UUID userId);

  /** Pedido mais recente do menor (aberto, autorizado, recusado ou cancelado). */
  Optional<GuardianConsent> latest(UUID userId);

  void save(GuardianConsent consent, RequestOrigin origin);

  /** Grava o novo link (só o hash do segredo) do pedido aberto. */
  void replaceLink(GuardianConsent consent, GuardianLinkToken token);

  void cancel(GuardianConsent consent);

  /** Pedido aberto e com link válido, pelo segredo; inclui o primeiro nome do menor. */
  Optional<LinkedRequest> findByLink(GuardianLinkToken token);

  /**
   * Grava a decisão do responsável se o link ainda estiver aberto.
   *
   * @return false se o link deixou de valer entre a leitura e a gravação
   */
  boolean recordDecision(GuardianConsent decided, GuardianLinkToken token, RequestOrigin origin);

  record LinkedRequest(GuardianConsent consent, String minorFirstName) {}
}
