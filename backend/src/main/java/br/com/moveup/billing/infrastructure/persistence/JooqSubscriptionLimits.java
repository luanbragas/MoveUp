package br.com.moveup.billing.infrastructure.persistence;

import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.PLAN;
import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.SUBSCRIPTION;

import br.com.moveup.billing.application.port.out.SubscriptionLimits;
import java.util.OptionalInt;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

@Repository
public class JooqSubscriptionLimits implements SubscriptionLimits {

  private final DSLContext dsl;

  public JooqSubscriptionLimits(DSLContext dsl) {
    this.dsl = dsl;
  }

  @Override
  public OptionalInt liveLimit(UUID organizationId, boolean lock) {
    var query =
        dsl.select(PLAN.MAX_ACTIVE_CLIENTS)
            .from(SUBSCRIPTION)
            .join(PLAN)
            .on(PLAN.ID.eq(SUBSCRIPTION.PLAN_ID))
            .where(SUBSCRIPTION.ORGANIZATION_ID.eq(organizationId))
            .and(SUBSCRIPTION.STATUS.in("trialing", "active", "past_due"));
    var limit =
        lock
            ? query.forUpdate().of(SUBSCRIPTION).fetchOptional(PLAN.MAX_ACTIVE_CLIENTS)
            : query.fetchOptional(PLAN.MAX_ACTIVE_CLIENTS);
    return limit.map(OptionalInt::of).orElseGet(OptionalInt::empty);
  }
}
