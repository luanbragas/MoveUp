package br.com.moveup.accounts.infrastructure.persistence;

import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.APP_USER;
import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.AUTH_IDENTITY;

import br.com.moveup.shared.infrastructure.security.AppUserResolver;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

/**
 * {@code auth_identity} → {@code app_user}, ignorando contas anonimizadas. Roda uma vez por
 * requisição, antes da transação do caso de uso. Essas duas tabelas não têm RLS porque não guardam
 * dado de saúde.
 */
@Repository
public class JooqAppUserResolver implements AppUserResolver {

  private final DSLContext dsl;

  public JooqAppUserResolver(DSLContext dsl) {
    this.dsl = dsl;
  }

  @Override
  public Optional<UUID> resolve(String provider, String subject) {
    return dsl.select(APP_USER.ID)
        .from(AUTH_IDENTITY)
        .join(APP_USER)
        .on(APP_USER.ID.eq(AUTH_IDENTITY.USER_ID))
        .where(AUTH_IDENTITY.PROVIDER.eq(provider))
        .and(AUTH_IDENTITY.SUBJECT.eq(subject))
        .and(APP_USER.ANONYMIZED_AT.isNull())
        .fetchOptional(APP_USER.ID);
  }
}
