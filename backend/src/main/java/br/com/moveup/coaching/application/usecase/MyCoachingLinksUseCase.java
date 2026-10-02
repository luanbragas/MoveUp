package br.com.moveup.coaching.application.usecase;

import br.com.moveup.accounts.api.AccountDirectory;
import br.com.moveup.accounts.api.AccountDirectory.ProfessionalCard;
import br.com.moveup.coaching.application.port.in.MyCoachingLinks;
import br.com.moveup.coaching.application.port.out.CoachingLinks;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class MyCoachingLinksUseCase implements MyCoachingLinks {

  private static final ProfessionalCard UNKNOWN = new ProfessionalCard("Profissional", "");

  private final CoachingLinks links;
  private final AccountDirectory accounts;

  public MyCoachingLinksUseCase(CoachingLinks links, AccountDirectory accounts) {
    this.links = links;
    this.accounts = accounts;
  }

  @Override
  @Transactional(readOnly = true)
  public List<MyLink> handle(UUID userId) {
    return links.ofClientUser(userId).stream()
        .map(
            link -> {
              var card = accounts.professionalCard(link.professionalId()).orElse(UNKNOWN);
              return new MyLink(
                  link.linkId(),
                  link.status(),
                  link.startedAt(),
                  card.name(),
                  card.organizationName());
            })
        .toList();
  }
}
