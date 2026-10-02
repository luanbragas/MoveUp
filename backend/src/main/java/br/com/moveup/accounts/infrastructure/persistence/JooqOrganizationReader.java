package br.com.moveup.accounts.infrastructure.persistence;

import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.ORGANIZATION;

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
}
