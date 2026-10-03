package br.com.moveup.accounts.fakes;

import br.com.moveup.accounts.application.port.in.RequestOrigin;
import br.com.moveup.accounts.application.port.out.ConsentRepository;
import br.com.moveup.accounts.application.port.out.GuardianConsentRepository;
import br.com.moveup.accounts.domain.model.ConsentGrant;
import br.com.moveup.accounts.domain.model.ConsentKind;
import br.com.moveup.accounts.domain.model.GuardianConsent;
import br.com.moveup.accounts.domain.model.GuardianLinkToken;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Consentimentos (do usuário e do responsável) em memória. */
public final class InMemoryConsents implements ConsentRepository, GuardianConsentRepository {

  private final Map<UUID, Map<ConsentKind, String>> active = new HashMap<>();
  private final Map<UUID, GuardianConsent> guardianById = new HashMap<>();
  private final Map<UUID, byte[]> tokenHashById = new HashMap<>();
  public final List<GuardianConsent> savedGuardianRequests = new ArrayList<>();
  public RequestOrigin lastOrigin;
  public RequestOrigin lastDecisionOrigin;

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
  public boolean hasVerified(UUID userId) {
    return guardianById.values().stream()
        .anyMatch(c -> c.userId().equals(userId) && c.status() == GuardianConsent.Status.VERIFIED);
  }

  @Override
  public Optional<GuardianConsent> latest(UUID userId) {
    return guardianById.values().stream()
        .filter(c -> c.userId().equals(userId))
        .max(Comparator.comparing(GuardianConsent::requestedAt));
  }

  @Override
  public void save(GuardianConsent consent, RequestOrigin origin) {
    guardianById.put(consent.id(), consent);
    savedGuardianRequests.add(consent);
    lastOrigin = origin;
  }

  @Override
  public void replaceLink(GuardianConsent consent, GuardianLinkToken token) {
    guardianById.put(consent.id(), consent);
    tokenHashById.put(consent.id(), token.hash());
  }

  @Override
  public void cancel(GuardianConsent consent) {
    guardianById.put(consent.id(), consent);
    tokenHashById.remove(consent.id());
  }

  @Override
  public Optional<LinkedRequest> findByLink(GuardianLinkToken token) {
    var hash = token.hash();
    return tokenHashById.entrySet().stream()
        .filter(e -> Arrays.equals(e.getValue(), hash))
        .map(e -> guardianById.get(e.getKey()))
        .filter(c -> c.status() == GuardianConsent.Status.PENDING)
        .findFirst()
        .map(c -> new LinkedRequest(c, "Bia"));
  }

  @Override
  public boolean recordDecision(
      GuardianConsent decided, GuardianLinkToken token, RequestOrigin origin) {
    if (findByLink(token).isEmpty()) {
      return false;
    }
    guardianById.put(decided.id(), decided);
    tokenHashById.remove(decided.id());
    lastDecisionOrigin = origin;
    return true;
  }
}
