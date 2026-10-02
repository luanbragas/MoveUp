package br.com.moveup.billing.infrastructure.persistence;

import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.PLAN;
import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.SUBSCRIPTION;

import br.com.moveup.billing.application.port.out.TrialSubscriptions;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

@Repository
public class JooqTrialSubscriptions implements TrialSubscriptions {

  private final DSLContext dsl;

  public JooqTrialSubscriptions(DSLContext dsl) {
    this.dsl = dsl;
  }

  @Override
  public void startTrial(UUID organizationId, String planCode, Instant trialEndsAt) {
    var planId =
        dsl.select(PLAN.ID)
            .from(PLAN)
            .where(PLAN.CODE.eq(planCode))
            .and(PLAN.IS_ACTIVE.isTrue())
            .fetchOptional(PLAN.ID)
            .orElseThrow(
                () -> new IllegalStateException("plano de teste não encontrado: " + planCode));
    dsl.insertInto(SUBSCRIPTION)
        .set(SUBSCRIPTION.ORGANIZATION_ID, organizationId)
        .set(SUBSCRIPTION.PLAN_ID, planId)
        .set(SUBSCRIPTION.STATUS, "trialing")
        .set(SUBSCRIPTION.TRIAL_ENDS_AT, trialEndsAt.atOffset(ZoneOffset.UTC))
        .execute();
  }
}
