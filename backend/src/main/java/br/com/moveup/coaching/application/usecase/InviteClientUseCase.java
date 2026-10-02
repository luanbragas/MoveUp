package br.com.moveup.coaching.application.usecase;

import br.com.moveup.accounts.api.AccountDirectory;
import br.com.moveup.billing.api.PlanLimits;
import br.com.moveup.coaching.application.port.in.InviteClient;
import br.com.moveup.coaching.application.port.out.CoachingLinks;
import br.com.moveup.coaching.domain.exception.CoachingConflict;
import br.com.moveup.coaching.domain.model.ClientPreRegistration;
import br.com.moveup.shared.domain.Forbidden;
import br.com.moveup.shared.domain.IdGenerator;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pré-cadastro + vínculo pendente + convite. A vaga do plano é conferida aqui (aviso cedo) e de
 * novo no aceite, com trava, porque o limite pode ser atingido entre um e outro.
 */
public class InviteClientUseCase implements InviteClient {

  private final AccountDirectory accounts;
  private final PlanLimits planLimits;
  private final CoachingLinks links;
  private final InvitationIssuer issuer;
  private final IdGenerator ids;

  public InviteClientUseCase(
      AccountDirectory accounts,
      PlanLimits planLimits,
      CoachingLinks links,
      InvitationIssuer issuer,
      IdGenerator ids) {
    this.accounts = accounts;
    this.planLimits = planLimits;
    this.links = links;
    this.issuer = issuer;
    this.ids = ids;
  }

  @Override
  @Transactional
  public Invitation handle(UUID professionalId, NewClient client) {
    var organizationId = accounts.organizationOf(professionalId).orElseThrow(Forbidden::new);
    var limit = planLimits.activeClientLimit(organizationId);
    if (limit.isEmpty() || links.countActive(organizationId) >= limit.getAsInt()) {
      throw CoachingConflict.planLimitReached();
    }
    var preRegistration =
        ClientPreRegistration.of(client.name(), client.email(), client.phone(), client.goal());
    var clientId = ids.newId();
    var linkId = ids.newId();
    links.createPending(clientId, linkId, organizationId, professionalId, preRegistration);
    return issuer.issue(clientId, linkId);
  }
}
