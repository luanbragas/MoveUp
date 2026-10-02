package br.com.moveup.accounts.infrastructure.persistence;

import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.APP_USER;
import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.ORGANIZATION;

import br.com.moveup.accounts.api.AccountDirectory.ProfessionalCard;
import br.com.moveup.accounts.application.port.out.OrganizationReader;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

@Repository
public class JooqOrganizationReader implements OrganizationReader {

  private final DSLContext dsl;

  public JooqOrganizationReader(DSLContext dsl) {
    this.dsl = dsl;
  }

  @Override
  public Optional<UUID> ownedBy(UUID userId) {
    return dsl.select(ORGANIZATION.ID)
        .from(ORGANIZATION)
        .where(ORGANIZATION.OWNER_USER_ID.eq(userId))
        .orderBy(ORGANIZATION.CREATED_AT)
        .limit(1)
        .fetchOptional(ORGANIZATION.ID);
  }

  @Override
  public Optional<ProfessionalCard> card(UUID professionalUserId) {
    return dsl.select(APP_USER.NAME, ORGANIZATION.NAME)
        .from(ORGANIZATION)
        .join(APP_USER)
        .on(APP_USER.ID.eq(ORGANIZATION.OWNER_USER_ID))
        .where(ORGANIZATION.OWNER_USER_ID.eq(professionalUserId))
        .and(APP_USER.ANONYMIZED_AT.isNull())
        .orderBy(ORGANIZATION.CREATED_AT)
        .limit(1)
        .fetchOptional(r -> new ProfessionalCard(r.value1(), r.value2()));
  }
}
