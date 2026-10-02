package br.com.moveup.accounts.infrastructure.persistence;

import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.CONSENT;

import br.com.moveup.accounts.application.port.in.RequestOrigin;
import br.com.moveup.accounts.application.port.out.ConsentRepository;
import br.com.moveup.accounts.domain.model.ConsentGrant;
import br.com.moveup.accounts.domain.model.ConsentKind;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

@Repository
public class JooqConsentRepository implements ConsentRepository {

  private final DSLContext dsl;

  public JooqConsentRepository(DSLContext dsl) {
    this.dsl = dsl;
  }

  @Override
  public Map<ConsentKind, String> acceptedVersions(UUID userId) {
    var accepted = new EnumMap<ConsentKind, String>(ConsentKind.class);
    // do mais antigo para o mais recente: o último aceite de cada tipo é o que fica no mapa
    dsl.select(CONSENT.KIND, CONSENT.DOC_VERSION)
        .from(CONSENT)
        .where(CONSENT.USER_ID.eq(userId))
        .and(CONSENT.REVOKED_AT.isNull())
        .orderBy(CONSENT.GRANTED_AT, CONSENT.ID)
        .fetch()
        .forEach(
            r ->
                accepted.put(
                    ConsentKind.fromCode(r.get(CONSENT.KIND)), r.get(CONSENT.DOC_VERSION)));
    return accepted;
  }

  @Override
  public void grant(UUID userId, List<ConsentGrant> grants, RequestOrigin origin) {
    for (var grant : grants) {
      dsl.insertInto(CONSENT)
          .set(CONSENT.USER_ID, userId)
          .set(CONSENT.KIND, grant.kind().code())
          .set(CONSENT.DOC_VERSION, grant.docVersion())
          .set(CONSENT.IP.coerce(String.class), InetValues.inet(origin.ip()))
          .set(CONSENT.USER_AGENT, origin.userAgent())
          .execute();
    }
  }

  @Override
  public boolean revoke(UUID userId, ConsentKind kind, RequestOrigin origin) {
    return dsl.update(CONSENT)
            .set(CONSENT.REVOKED_AT, DSL.currentOffsetDateTime())
            .set(CONSENT.REVOKED_IP.coerce(String.class), InetValues.inet(origin.ip()))
            .where(CONSENT.USER_ID.eq(userId))
            .and(CONSENT.KIND.eq(kind.code()))
            .and(CONSENT.REVOKED_AT.isNull())
            .execute()
        > 0;
  }
}
