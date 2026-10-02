package br.com.moveup.accounts.fakes;

import br.com.moveup.accounts.application.port.in.RequestOrigin;
import br.com.moveup.accounts.application.port.out.ConsentRepository;
import br.com.moveup.accounts.application.port.out.GuardianConsentRepository;
import br.com.moveup.accounts.domain.model.ConsentGrant;
import br.com.moveup.accounts.domain.model.ConsentKind;
import br.com.moveup.accounts.domain.model.GuardianConsent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Consentimentos (do usuário e do responsável) em memória. */
public final class InMemoryConsents implements ConsentRepository, GuardianConsentRepository {

  private final Map<UUID, Map<ConsentKind, String>> active = new HashMap<>();
  public final List<GuardianConsent> guardianConsents = new ArrayList<>();
  public RequestOrigin lastOrigin;

  @Override
  public Map<ConsentKind, String> acceptedVersions(UUID userId) {
    var byKind = active.get(userId);
    return byKind == null ? new EnumMap<>(ConsentKind.class) : new EnumMap<>(byKind);
  }

  @Override
  public void grant(UUID userId, List<ConsentGrant> grants, RequestOrigin origin) {
    var byKind = active.computeIfAbsent(userId, id -> new EnumMap<>(ConsentKind.class));
    grants.forEach(g -> byKind.put(g.kind(), g.docVersion()));
    lastOrigin = origin;
  }

  @Override
  public boolean revoke(UUID userId, ConsentKind kind, RequestOrigin origin) {
    var byKind = active.get(userId);
    return byKind != null && byKind.remove(kind) != null;
  }

  @Override
  public boolean hasActive(UUID userId) {
    return guardianConsents.stream().anyMatch(c -> c.userId().equals(userId));
  }

  @Override
  public void save(GuardianConsent consent, RequestOrigin origin) {
    guardianConsents.add(consent);
    lastOrigin = origin;
  }
}
