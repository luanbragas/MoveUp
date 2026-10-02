package br.com.moveup.accounts.infrastructure.persistence;

import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.GUARDIAN_CONSENT;

import br.com.moveup.accounts.application.port.in.RequestOrigin;
import br.com.moveup.accounts.application.port.out.GuardianConsentRepository;
import br.com.moveup.accounts.domain.model.GuardianConsent;
import java.time.ZoneOffset;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

/** Tabela com RLS: só o próprio usuário (app.user_id da transação) lê e grava. */
@Repository
public class JooqGuardianConsentRepository implements GuardianConsentRepository {

  private final DSLContext dsl;

  public JooqGuardianConsentRepository(DSLContext dsl) {
    this.dsl = dsl;
  }

  @Override
  public boolean hasActive(UUID userId) {
    return dsl.fetchExists(
        GUARDIAN_CONSENT,
        GUARDIAN_CONSENT.USER_ID.eq(userId),
        GUARDIAN_CONSENT.REVOKED_AT.isNull());
  }

  @Override
  public void save(GuardianConsent consent, RequestOrigin origin) {
    dsl.insertInto(GUARDIAN_CONSENT)
        .set(GUARDIAN_CONSENT.ID, consent.id())
        .set(GUARDIAN_CONSENT.USER_ID, consent.userId())
        .set(GUARDIAN_CONSENT.GUARDIAN_NAME, consent.guardianName().value())
        .set(GUARDIAN_CONSENT.GUARDIAN_EMAIL, consent.guardianEmail().value())
        .set(GUARDIAN_CONSENT.RELATIONSHIP, consent.relationship().code())
        .set(GUARDIAN_CONSENT.DOC_VERSION, consent.docVersion())
        .set(GUARDIAN_CONSENT.GRANTED_AT, consent.grantedAt().atOffset(ZoneOffset.UTC))
        .set(GUARDIAN_CONSENT.IP.coerce(String.class), InetValues.inet(origin.ip()))
        .set(GUARDIAN_CONSENT.USER_AGENT, origin.userAgent())
        .execute();
  }
}
