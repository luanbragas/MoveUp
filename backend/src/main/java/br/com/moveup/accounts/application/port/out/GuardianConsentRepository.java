package br.com.moveup.accounts.application.port.out;

import br.com.moveup.accounts.application.port.in.RequestOrigin;
import br.com.moveup.accounts.domain.model.GuardianConsent;
import java.util.UUID;

public interface GuardianConsentRepository {

  boolean hasActive(UUID userId);

  void save(GuardianConsent consent, RequestOrigin origin);
}
