package br.com.moveup.audit.infrastructure.persistence;

import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.AUDIT_LOG;

import br.com.moveup.audit.api.AuditTrail;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Grava a trilha na transação de quem chama (nunca numa transação própria: se a mudança for
 * desfeita, o registro também é).
 */
@Repository
public class JooqAuditTrail implements AuditTrail {

  private final DSLContext dsl;

  public JooqAuditTrail(DSLContext dsl) {
    this.dsl = dsl;
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public void record(Entry entry) {
    dsl.insertInto(AUDIT_LOG)
        .set(AUDIT_LOG.ACTOR_ID, entry.actorId())
        .set(AUDIT_LOG.ENTITY, entry.entity())
        .set(AUDIT_LOG.ENTITY_ID, entry.entityId())
        .set(AUDIT_LOG.CLIENT_ID, entry.clientId())
        .set(AUDIT_LOG.ACTION, entry.action().code())
        .execute();
  }
}
