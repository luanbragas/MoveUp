package br.com.moveup.anamnesis.application.usecase;

import br.com.moveup.anamnesis.application.port.in.ManageRestrictions;
import br.com.moveup.anamnesis.application.port.out.Restrictions;
import br.com.moveup.anamnesis.domain.model.HealthRestriction;
import br.com.moveup.coaching.api.LinkDirectory;
import br.com.moveup.shared.domain.IdGenerator;
import br.com.moveup.shared.domain.ResourceNotFound;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class ManageRestrictionsUseCase implements ManageRestrictions {

  private final Restrictions restrictions;
  private final LinkDirectory links;
  private final IdGenerator ids;

  public ManageRestrictionsUseCase(
      Restrictions restrictions, LinkDirectory links, IdGenerator ids) {
    this.restrictions = restrictions;
    this.links = links;
    this.ids = ids;
  }

  @Override
  @Transactional(readOnly = true)
  public List<RestrictionView> list(UUID professionalId, UUID linkId, boolean includeResolved) {
    var link = links.ofProfessional(professionalId, linkId).orElseThrow(ResourceNotFound::new);
    return restrictions.list(link.clientId(), includeResolved).stream()
        .map(ManageRestrictionsUseCase::view)
        .toList();
  }

  @Override
  @Transactional
  public RestrictionView create(UUID professionalId, UUID linkId, RestrictionInput input) {
    var clientId = writable(professionalId, linkId);
    var restriction =
        new HealthRestriction(
            ids.newId(),
            clientId,
            input.kind(),
            input.bodyRegion(),
            input.description(),
            input.severity(),
            input.resolvedOn(),
            null);
    restrictions.insert(restriction, professionalId);
    return view(restriction);
  }

  @Override
  @Transactional
  public RestrictionView update(
      UUID professionalId, UUID linkId, UUID restrictionId, RestrictionInput input) {
    var clientId = writable(professionalId, linkId);
    var current = restrictions.find(clientId, restrictionId).orElseThrow(ResourceNotFound::new);
    var changed =
        new HealthRestriction(
            current.id(),
            clientId,
            input.kind(),
            input.bodyRegion(),
            input.description(),
            input.severity(),
            input.resolvedOn(),
            current.sourceAnamnesisId());
    restrictions.update(changed);
    return view(changed);
  }

  @Override
  @Transactional
  public void delete(UUID professionalId, UUID linkId, UUID restrictionId) {
    var clientId = writable(professionalId, linkId);
    restrictions.find(clientId, restrictionId).orElseThrow(ResourceNotFound::new);
    restrictions.delete(clientId, restrictionId);
  }

  private UUID writable(UUID professionalId, UUID linkId) {
    var link = links.ofProfessional(professionalId, linkId).orElseThrow(ResourceNotFound::new);
    if (!link.acceptsTraining()) {
      throw new ResourceNotFound();
    }
    return link.clientId();
  }

  static RestrictionView view(HealthRestriction r) {
    return new RestrictionView(
        r.id(),
        r.kind(),
        r.bodyRegion(),
        r.description(),
        r.severity(),
        r.resolvedOn(),
        r.sourceAnamnesisId() != null);
  }
}
