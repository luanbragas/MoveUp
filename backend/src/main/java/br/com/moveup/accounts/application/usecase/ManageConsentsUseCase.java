package br.com.moveup.accounts.application.usecase;

import br.com.moveup.accounts.application.port.in.ManageConsents;
import br.com.moveup.accounts.application.port.in.RequestOrigin;
import br.com.moveup.accounts.application.port.out.ConsentRepository;
import br.com.moveup.accounts.application.port.out.LegalDocuments;
import br.com.moveup.accounts.domain.exception.InvalidAccountData;
import br.com.moveup.accounts.domain.model.ConsentGrant;
import br.com.moveup.accounts.domain.model.ConsentKind;
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class ManageConsentsUseCase implements ManageConsents {

  private final ConsentRepository consents;
  private final LegalDocuments legalDocuments;

  public ManageConsentsUseCase(ConsentRepository consents, LegalDocuments legalDocuments) {
    this.consents = consents;
    this.legalDocuments = legalDocuments;
  }

  @Override
  @Transactional
  public void grant(UUID userId, List<Grant> grants, RequestOrigin origin) {
    if (grants.isEmpty()) {
      throw new InvalidAccountData("consents-empty", "Nenhum consentimento informado.");
    }
    var current = legalDocuments.current();
    var accepted =
        grants.stream()
            .map(g -> ConsentGrant.accept(parseKind(g.kind()), g.docVersion(), current))
            .toList();
    consents.grant(userId, accepted, origin);
  }

  @Override
  @Transactional
  public void revoke(UUID userId, String kind, RequestOrigin origin) {
    consents.revoke(userId, parseKind(kind), origin);
  }

  @Override
  public LegalVersionsView currentVersions() {
    var current = legalDocuments.current();
    var byCode = new TreeMap<String, String>();
    current.consents().forEach((kind, version) -> byCode.put(kind.code(), version));
    return new LegalVersionsView(byCode, current.guardianConsent());
  }

  private static ConsentKind parseKind(String kind) {
    try {
      return ConsentKind.fromCode(kind);
    } catch (IllegalArgumentException e) {
      throw new InvalidAccountData("consent-kind-invalid", "Tipo de consentimento inválido.");
    }
  }
}
