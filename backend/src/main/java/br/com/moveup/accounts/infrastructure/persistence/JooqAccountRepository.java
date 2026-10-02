package br.com.moveup.accounts.infrastructure.persistence;

import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.APP_USER;
import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.AUTH_IDENTITY;
import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.ORGANIZATION;
import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.ORGANIZATION_MEMBER;
import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.PROFESSIONAL_PROFILE;

import br.com.moveup.accounts.application.port.out.AccountRepository;
import br.com.moveup.accounts.domain.model.Account;
import br.com.moveup.accounts.domain.model.BirthDate;
import br.com.moveup.accounts.domain.model.Email;
import br.com.moveup.accounts.domain.model.LoginIdentity;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

@Repository
public class JooqAccountRepository implements AccountRepository {

  private final DSLContext dsl;

  public JooqAccountRepository(DSLContext dsl) {
    this.dsl = dsl;
  }

  @Override
  public boolean existsByIdentityOrEmail(LoginIdentity identity, Email email) {
    return dsl.fetchExists(
            AUTH_IDENTITY,
            AUTH_IDENTITY.PROVIDER.eq(identity.provider()),
            AUTH_IDENTITY.SUBJECT.eq(identity.subject()))
        // citext: a comparação no banco já ignora caixa
        || dsl.fetchExists(APP_USER, APP_USER.EMAIL.eq(DSL.val(email.value())));
  }

  @Override
  public void save(Account account) {
    dsl.insertInto(APP_USER)
        .set(APP_USER.ID, account.id())
        .set(APP_USER.NAME, account.name().value())
        .set(APP_USER.EMAIL, account.email().value())
        .set(APP_USER.ROLE, account.role().code())
        .set(APP_USER.BIRTH_DATE, account.birthDate().map(BirthDate::value).orElse(null))
        .execute();
    dsl.insertInto(AUTH_IDENTITY)
        .set(AUTH_IDENTITY.PROVIDER, account.identity().provider())
        .set(AUTH_IDENTITY.SUBJECT, account.identity().subject())
        .set(AUTH_IDENTITY.USER_ID, account.id())
        .set(AUTH_IDENTITY.LAST_LOGIN_AT, DSL.currentOffsetDateTime())
        .execute();
    account
        .professional()
        .ifPresent(
            setup ->
                dsl.insertInto(PROFESSIONAL_PROFILE)
                    .set(PROFESSIONAL_PROFILE.USER_ID, account.id())
                    .set(PROFESSIONAL_PROFILE.REGISTRY_NUMBER, setup.registryNumber().orElse(null))
                    .execute());
  }

  @Override
  public void createOrganization(UUID organizationId, Account owner) {
    var businessName =
        owner
            .professional()
            .orElseThrow(() -> new IllegalArgumentException("só profissional tem organização"))
            .businessName();
    dsl.insertInto(ORGANIZATION)
        .set(ORGANIZATION.ID, organizationId)
        .set(ORGANIZATION.NAME, businessName)
        .set(ORGANIZATION.OWNER_USER_ID, owner.id())
        .execute();
    dsl.insertInto(ORGANIZATION_MEMBER)
        .set(ORGANIZATION_MEMBER.ORGANIZATION_ID, organizationId)
        .set(ORGANIZATION_MEMBER.USER_ID, owner.id())
        .set(ORGANIZATION_MEMBER.ROLE, "owner")
        .execute();
  }
}
