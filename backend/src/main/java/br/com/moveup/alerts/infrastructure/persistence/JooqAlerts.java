package br.com.moveup.alerts.infrastructure.persistence;

import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.ALERT;

import br.com.moveup.alerts.application.port.out.Alerts;
import br.com.moveup.alerts.domain.model.NewAlert;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** Alertas. Na API o RLS restringe ao profissional; no worker, app_worker vê todos. */
@Repository
public class JooqAlerts implements Alerts {

  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final TypeReference<LinkedHashMap<String, Integer>> FACTS =
      new TypeReference<>() {};

  private final DSLContext dsl;

  public JooqAlerts(DSLContext dsl) {
    this.dsl = dsl;
  }

  @Override
  public Optional<UUID> openIfNew(NewAlert alert) {
    // índice one_open_alert (professional_id, dedupe_key) where status <> 'resolved'
    return dsl.insertInto(ALERT)
        .set(ALERT.ORGANIZATION_ID, alert.organizationId())
        .set(ALERT.PROFESSIONAL_ID, alert.professionalId())
        .set(ALERT.CLIENT_ID, alert.clientId())
        .set(ALERT.TYPE, alert.type().code())
        .set(ALERT.SEVERITY, alert.severity().code())
        .set(ALERT.FACTS, JSONB.valueOf(JSON.writeValueAsString(alert.facts())))
        .set(ALERT.DEDUPE_KEY, alert.dedupeKey())
        .onConflict(ALERT.PROFESSIONAL_ID, ALERT.DEDUPE_KEY)
        .where(ALERT.STATUS.ne("resolved"))
        .doNothing()
        .returningResult(ALERT.ID)
        .fetchOptional(ALERT.ID);
  }

  @Override
  public void resolveOpen(UUID professionalId, String dedupeKey, Instant now) {
    dsl.update(ALERT)
        .set(ALERT.STATUS, "resolved")
        .set(ALERT.RESOLVED_AT, now.atOffset(ZoneOffset.UTC))
        .set(ALERT.SNOOZED_UNTIL, (java.time.OffsetDateTime) null)
        .set(ALERT.UPDATED_AT, now.atOffset(ZoneOffset.UTC))
        .where(ALERT.PROFESSIONAL_ID.eq(professionalId))
        .and(ALERT.DEDUPE_KEY.eq(dedupeKey))
        .and(ALERT.STATUS.ne("resolved"))
        .execute();
  }

  @Override
  public List<AlertView> list(
      UUID professionalId, String status, UUID before, int limit, Instant now) {
    var at = now.atOffset(ZoneOffset.UTC);
    Condition filter =
        switch (status) {
          case "open" ->
              ALERT
                  .STATUS
                  .eq("open")
                  .or(ALERT.STATUS.eq("snoozed").and(ALERT.SNOOZED_UNTIL.le(at)));
          case "snoozed" -> ALERT.STATUS.eq("snoozed").and(ALERT.SNOOZED_UNTIL.gt(at));
          default -> ALERT.STATUS.eq("resolved");
        };
    var query = dsl.selectFrom(ALERT).where(ALERT.PROFESSIONAL_ID.eq(professionalId)).and(filter);
    if (before != null) {
      query = query.and(ALERT.ID.lt(before));
    }
    return query.orderBy(ALERT.ID.desc()).limit(limit).fetch(r -> view(r, now));
  }

  @Override
  public Optional<AlertView> find(UUID professionalId, UUID alertId) {
    return dsl.selectFrom(ALERT)
        .where(ALERT.ID.eq(alertId))
        .and(ALERT.PROFESSIONAL_ID.eq(professionalId))
        .fetchOptional(r -> view(r, Instant.now()));
  }

  @Override
  public void resolve(UUID alertId, Instant now) {
    dsl.update(ALERT)
        .set(ALERT.STATUS, "resolved")
        .set(ALERT.RESOLVED_AT, now.atOffset(ZoneOffset.UTC))
        .set(ALERT.SNOOZED_UNTIL, (java.time.OffsetDateTime) null)
        .set(ALERT.UPDATED_AT, now.atOffset(ZoneOffset.UTC))
        .where(ALERT.ID.eq(alertId))
        .execute();
  }

  @Override
  public void snooze(UUID alertId, Instant until) {
    dsl.update(ALERT)
        .set(ALERT.STATUS, "snoozed")
        .set(ALERT.SNOOZED_UNTIL, until.atOffset(ZoneOffset.UTC))
        .set(ALERT.UPDATED_AT, DSL.currentOffsetDateTime())
        .where(ALERT.ID.eq(alertId))
        .execute();
  }

  @Override
  public int wakeSnoozed(Instant now) {
    return dsl.update(ALERT)
        .set(ALERT.STATUS, "open")
        .set(ALERT.SNOOZED_UNTIL, (java.time.OffsetDateTime) null)
        .set(ALERT.UPDATED_AT, now.atOffset(ZoneOffset.UTC))
        .where(ALERT.STATUS.eq("snoozed"))
        .and(ALERT.SNOOZED_UNTIL.le(now.atOffset(ZoneOffset.UTC)))
        .execute();
  }

  @Override
  public int countOpen(UUID professionalId, Instant now) {
    var at = now.atOffset(ZoneOffset.UTC);
    return dsl.fetchCount(
        ALERT,
        ALERT
            .PROFESSIONAL_ID
            .eq(professionalId)
            .and(
                ALERT
                    .STATUS
                    .eq("open")
                    .or(ALERT.STATUS.eq("snoozed").and(ALERT.SNOOZED_UNTIL.le(at)))));
  }

  private static AlertView view(Record r, Instant now) {
    var a = r.into(ALERT);
    var snoozed = a.getSnoozedUntil() == null ? null : a.getSnoozedUntil().toInstant();
    // adiado que venceu aparece como aberto antes mesmo do job diário
    var status =
        "snoozed".equals(a.getStatus()) && snoozed != null && !snoozed.isAfter(now)
            ? "open"
            : a.getStatus();
    Map<String, Integer> facts = JSON.readValue(a.getFacts().data(), FACTS);
    return new AlertView(
        a.getId(),
        a.getType(),
        a.getSeverity(),
        status,
        a.getClientId(),
        facts,
        a.getCreatedAt().toInstant(),
        "open".equals(status) ? null : snoozed,
        a.getResolvedAt() == null ? null : a.getResolvedAt().toInstant());
  }
}
