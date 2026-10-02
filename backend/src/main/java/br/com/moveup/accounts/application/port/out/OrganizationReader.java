package br.com.moveup.accounts.application.port.out;

import br.com.moveup.accounts.api.AccountDirectory.ProfessionalCard;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationReader {

  /** Organização em que o usuário é dono (MVP: uma por profissional). */
  Optional<UUID> ownedBy(UUID userId);

  /** Nome do profissional e da organização dele. */
  Optional<ProfessionalCard> card(UUID professionalUserId);
}
