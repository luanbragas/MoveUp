package br.com.moveup.coaching.application.usecase;

import br.com.moveup.audit.api.AuditTrail;
import br.com.moveup.audit.api.AuditTrail.Action;
import br.com.moveup.billing.api.PlanLimits;
import br.com.moveup.coaching.application.port.in.InviteClient.Invitation;
import br.com.moveup.coaching.application.port.in.ManageLink;
import br.com.moveup.coaching.application.port.out.CoachingLinks;
import br.com.moveup.coaching.application.port.out.Invites;
import br.com.moveup.coaching.domain.model.CoachingLink;
import br.com.moveup.shared.domain.ResourceNotFound;
import java.time.Clock;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * Convite e ciclo de vida do vínculo pelo profissional dono; o aluno só encerra o dele. Vínculo de
 * outro profissional responde 404 (não revela que existe). Toda mudança vai para o audit_log na
 * mesma transação.
 */
public class ManageLinkUseCase implements ManageLink {

  private static final String ENTITY = "coaching_link";

  private final CoachingLinks links;
  private final Invites invites;
  private final InvitationIssuer issuer;
  private final PlanLimits planLimits;
  private final AuditTrail audit;
  private final Clock clock;

  public ManageLinkUseCase(
      CoachingLinks links,
      Invites invites,
      InvitationIssuer issuer,
      PlanLimits planLimits,
      AuditTrail audit,
      Clock clock) {
    this.links = links;
    this.invites = invites;
    this.issuer = issuer;
    this.planLimits = planLimits;
    this.audit = audit;
    this.clock = clock;
  }

  @Override
  @Transactional
  public Invitation resendInvite(UUID professionalId, UUID linkId) {
    var link = owned(professionalId, linkId);
    link.requirePending();
    invites.revokePending(linkId);
    return issuer.issue(link.clientId(), linkId);
  }

  @Override
  @Transactional
  public void cancelInvite(UUID professionalId, UUID linkId) {
    owned(professionalId, linkId).requirePending();
    invites.revokePending(linkId);
  }

  @Override
  @Transactional
  public void inactivate(UUID professionalId, UUID linkId) {
    var link = owned(professionalId, linkId);
    link.inactivate();
    save(professionalId, link, Action.LINK_CHANGED);
  }

  @Override
  @Transactional
  public void reactivate(UUID professionalId, UUID linkId) {
    var link = owned(professionalId, linkId);
    // trava primeiro, conta depois: duas reativações simultâneas ficam em fila
    var limit = planLimits.lockActiveClientLimit(link.organizationId());
    link.reactivate(links.countActive(link.organizationId()), limit);
    save(professionalId, link, Action.LINK_CHANGED);
  }

  @Override
  @Transactional
  public void end(UUID professionalId, UUID linkId) {
    var link = owned(professionalId, linkId);
    link.end(clock.instant());
    invites.revokePending(linkId);
    save(professionalId, link, Action.LINK_ENDED);
  }

  @Override
  @Transactional
  public void endAsClient(UUID userId, UUID linkId) {
    invites.endAsClient(linkId); // a função confere o dono e grava o audit_log
  }

  private CoachingLink owned(UUID professionalId, UUID linkId) {
    return links
        .find(linkId)
        .filter(link -> link.belongsTo(professionalId))
        .orElseThrow(ResourceNotFound::new);
  }

  private void save(UUID actorId, CoachingLink link, Action action) {
    links.saveStatus(link);
    audit.record(new AuditTrail.Entry(actorId, ENTITY, link.id(), link.clientId(), action));
  }
}
