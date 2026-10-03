package br.com.moveup.anamnesis.infrastructure.persistence;

import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.HEALTH_RESTRICTION;

import br.com.moveup.anamnesis.application.port.out.Restrictions;
import br.com.moveup.anamnesis.domain.model.HealthRestriction;
import br.com.moveup.shared.application.crypto.FieldCipher;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

/** Restrições (RLS do aluno; descrição cifrada; excluir preenche deleted_at). */
@Repository
public class JooqRestrictions implements Restrictions {

  static final String DESCRIPTION_FIELD = "health_restriction.description";

  private final DSLContext dsl;
  private final FieldCipher cipher;

  public JooqRestrictions(DSLContext dsl, FieldCipher cipher) {
    this.dsl = dsl;
    this.cipher = cipher;
  }

  @Override
  public List<HealthRestriction> list(UUID clientId, boolean includeResolved) {
    var query =
        dsl.selectFrom(HEALTH_RESTRICTION)
            .where(HEALTH_RESTRICTION.CLIENT_ID.eq(clientId))
            .and(HEALTH_RESTRICTION.DELETED_AT.isNull());
    if (!includeResolved) {
      query = query.and(HEALTH_RESTRICTION.RESOLVED_ON.isNull());
    }
    return query
        .orderBy(HEALTH_RESTRICTION.RESOLVED_ON.asc().nullsFirst(), HEALTH_RESTRICTION.ID.desc())
        .fetch(this::toDomain);
  }

  @Override
  public Optional<HealthRestriction> find(UUID clientId, UUID restrictionId) {
    return dsl.selectFrom(HEALTH_RESTRICTION)
        .where(HEALTH_RESTRICTION.ID.eq(restrictionId))
        .and(HEALTH_RESTRICTION.CLIENT_ID.eq(clientId))
        .and(HEALTH_RESTRICTION.DELETED_AT.isNull())
        .fetchOptional(this::toDomain);
  }

  @Override
  public void insert(HealthRestriction r, UUID createdBy) {
    dsl.insertInto(HEALTH_RESTRICTION)
        .set(HEALTH_RESTRICTION.ID, r.id())
        .set(HEALTH_RESTRICTION.CLIENT_ID, r.clientId())
        .set(HEALTH_RESTRICTION.SOURCE_ANAMNESIS_ID, r.sourceAnamnesisId())
        .set(HEALTH_RESTRICTION.KIND, r.kind())
        .set(HEALTH_RESTRICTION.BODY_REGION, r.bodyRegion())
        .set(
            HEALTH_RESTRICTION.DESCRIPTION,
            cipher.encrypt(r.clientId(), DESCRIPTION_FIELD, r.description()))
        .set(HEALTH_RESTRICTION.SEVERITY, r.severity() == null ? null : r.severity().shortValue())
        .set(HEALTH_RESTRICTION.RESOLVED_ON, r.resolvedOn())
        .set(HEALTH_RESTRICTION.CREATED_BY, createdBy)
        .execute();
  }

  @Override
  public void update(HealthRestriction r) {
    dsl.update(HEALTH_RESTRICTION)
        .set(HEALTH_RESTRICTION.KIND, r.kind())
        .set(HEALTH_RESTRICTION.BODY_REGION, r.bodyRegion())
        .set(
            HEALTH_RESTRICTION.DESCRIPTION,
            cipher.encrypt(r.clientId(), DESCRIPTION_FIELD, r.description()))
        .set(HEALTH_RESTRICTION.SEVERITY, r.severity() == null ? null : r.severity().shortValue())
        .set(HEALTH_RESTRICTION.RESOLVED_ON, r.resolvedOn())
        .where(HEALTH_RESTRICTION.ID.eq(r.id()))
        .and(HEALTH_RESTRICTION.CLIENT_ID.eq(r.clientId()))
        .execute();
  }

  @Override
  public void delete(UUID clientId, UUID restrictionId) {
    dsl.update(HEALTH_RESTRICTION)
        .set(HEALTH_RESTRICTION.DELETED_AT, DSL.currentOffsetDateTime())
        .where(HEALTH_RESTRICTION.ID.eq(restrictionId))
        .and(HEALTH_RESTRICTION.CLIENT_ID.eq(clientId))
        .execute();
  }

  private HealthRestriction toDomain(Record record) {
    var r = record.into(HEALTH_RESTRICTION);
    return new HealthRestriction(
        r.getId(),
        r.getClientId(),
        r.getKind(),
        r.getBodyRegion(),
        cipher.decrypt(r.getClientId(), DESCRIPTION_FIELD, r.getDescription()),
        r.getSeverity() == null ? null : r.getSeverity().intValue(),
        r.getResolvedOn(),
        r.getSourceAnamnesisId());
  }
}
