package br.com.moveup.coaching.application.usecase;

import br.com.moveup.coaching.api.LinkDirectory;
import br.com.moveup.coaching.application.port.out.CoachingLinks;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class LinkDirectoryService implements LinkDirectory {

  private final CoachingLinks links;

  public LinkDirectoryService(CoachingLinks links) {
    this.links = links;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<LinkRef> ofProfessional(UUID professionalId, UUID linkId) {
    return links
        .find(linkId)
        .filter(link -> link.belongsTo(professionalId))
        .map(
            link ->
                new LinkRef(
                    link.id(), link.clientId(), link.organizationId(), link.status().code()));
  }
}
