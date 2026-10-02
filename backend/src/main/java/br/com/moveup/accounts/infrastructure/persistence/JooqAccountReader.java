package br.com.moveup.accounts.infrastructure.persistence;

import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.APP_USER;

import br.com.moveup.accounts.application.port.out.AccountReader;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

@Repository
public class JooqAccountReader implements AccountReader {

  private final DSLContext dsl;

  public JooqAccountReader(DSLContext dsl) {
    this.dsl = dsl;
  }

  @Override
  public Optional<AccountSummary> find(UUID userId) {
    return dsl.select(
            APP_USER.ID,
            APP_USER.NAME,
            APP_USER.EMAIL,
            APP_USER.LOCALE,
            APP_USER.TIMEZONE,
            APP_USER.WEIGHT_UNIT,
            APP_USER.LENGTH_UNIT,
            APP_USER.ROLE,
            APP_USER.BIRTH_DATE)
        .from(APP_USER)
        .where(APP_USER.ID.eq(userId))
        .and(APP_USER.ANONYMIZED_AT.isNull())
        .fetchOptional(
            r ->
                new AccountSummary(
                    r.value1(),
                    r.value2(),
                    r.value3(),
                    r.value4(),
                    r.value5(),
                    r.value6(),
                    r.value7(),
                    r.value8(),
                    r.value9()));
  }
}
