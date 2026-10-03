package br.com.moveup.accounts.infrastructure.persistence;

import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.GUARDIAN_CONSENT;

import br.com.moveup.accounts.application.port.in.RequestOrigin;
import br.com.moveup.accounts.application.port.out.GuardianConsentRepository;
import br.com.moveup.accounts.domain.model.GuardianConsent;
import br.com.moveup.accounts.domain.model.GuardianLinkToken;
import br.com.moveup.accounts.domain.model.GuardianRelationship;
import br.com.moveup.accounts.domain.model.PersonName;
import br.com.moveup.shared.infrastructure.persistence.jooq.tables.records.GuardianConsentRecord;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.springframework.stereotype.Repository;

/**
 * Lado do menor: tabela com RLS (só o próprio usuário, pelo app.user_id da transação). Lado do
 * responsável, sem conta: só as funções security definer da V16, pelo hash do segredo do link.
 */
@Repository
public class JooqGuardianConsentRepository implements GuardianConsentRepository {

  private final DSLContext dsl;

  public JooqGuardianConsentRepository(DSLContext dsl) {
    this.dsl = dsl;
  }

  @Override
  public boolean hasVerified(UUID userId) {
    return dsl.fetchExists(
        GUARDIAN_CONSENT,
        GUARDIAN_CONSENT.USER_ID.eq(userId),
        GUARDIAN_CONSENT.VERIFIED_AT.isNotNull(),
        GUARDIAN_CONSENT.REVOKED_AT.isNull());
  }

  @Override
  public Optional<GuardianConsent> latest(UUID userId) {
    return dsl.selectFrom(GUARDIAN_CONSENT)
        .where(GUARDIAN_CONSENT.USER_ID.eq(userId))
        .orderBy(GUARDIAN_CONSENT.GRANTED_AT.desc(), GUARDIAN_CONSENT.ID.desc())
        .limit(1)
        .fetchOptional(JooqGuardianConsentRepository::toDomain);
  }

  @Override
  public void save(GuardianConsent consent, RequestOrigin origin) {
    dsl.insertInto(GUARDIAN_CONSENT)
        .set(GUARDIAN_CONSENT.ID, consent.id())
        .set(GUARDIAN_CONSENT.USER_ID, consent.userId())
        .set(GUARDIAN_CONSENT.GUARDIAN_NAME, consent.guardianName().value())
        .set(GUARDIAN_CONSENT.RELATIONSHIP, consent.relationship().code())
        .set(GUARDIAN_CONSENT.DOC_VERSION, consent.docVersion())
        .set(GUARDIAN_CONSENT.GRANTED_AT, utc(consent.requestedAt()))
        .set(GUARDIAN_CONSENT.IP.coerce(String.class), InetValues.inet(origin.ip()))
        .set(GUARDIAN_CONSENT.USER_AGENT, origin.userAgent())
        .execute();
  }

  @Override
  public void replaceLink(GuardianConsent consent, GuardianLinkToken token) {
    dsl.update(GUARDIAN_CONSENT)
        .set(GUARDIAN_CONSENT.TOKEN_HASH, token.hash())
        .set(GUARDIAN_CONSENT.TOKEN_EXPIRES_AT, utc(consent.linkExpiresAt()))
        .where(GUARDIAN_CONSENT.ID.eq(consent.id()))
        .execute();
  }

  @Override
  public void cancel(GuardianConsent consent) {
    dsl.update(GUARDIAN_CONSENT)
        .set(GUARDIAN_CONSENT.REVOKED_AT, utc(consent.revokedAt()))
        .setNull(GUARDIAN_CONSENT.TOKEN_HASH)
        .setNull(GUARDIAN_CONSENT.TOKEN_EXPIRES_AT)
        .where(GUARDIAN_CONSENT.ID.eq(consent.id()))
        .execute();
  }

  @Override
  public Optional<LinkedRequest> findByLink(GuardianLinkToken token) {
    return dsl.resultQuery("select * from guardian_request_by_token({0})", DSL.val(token.hash()))
        .fetchOptional(JooqGuardianConsentRepository::toLinkedRequest);
  }

  @Override
  public boolean recordDecision(
      GuardianConsent decided, GuardianLinkToken token, RequestOrigin origin) {
    var decidedAt = decided.verifiedAt() != null ? decided.verifiedAt() : decided.declinedAt();
    var recorded =
        dsl.select(
                DSL.field(
                    "guardian_request_decide({0}, {1}, {2}, {3}, {4}, {5})",
                    SQLDataType.BOOLEAN,
                    DSL.val(token.hash()),
                    DSL.val(decided.status() == GuardianConsent.Status.VERIFIED),
                    DSL.val(decided.docVersion()),
                    DSL.val(utc(decidedAt)),
                    InetValues.inet(origin.ip()),
                    DSL.val(origin.userAgent(), String.class)))
            .fetchOne(0, Boolean.class);
    return Boolean.TRUE.equals(recorded);
  }

  private static GuardianConsent toDomain(GuardianConsentRecord r) {
    return new GuardianConsent(
        r.getId(),
        r.getUserId(),
        PersonName.of(r.getGuardianName()),
        GuardianRelationship.fromCode(r.getRelationship()),
        r.getDocVersion(),
        instant(r.getGrantedAt()),
        instant(r.getTokenExpiresAt()),
        instant(r.getVerifiedAt()),
        instant(r.getDeclinedAt()),
        instant(r.getRevokedAt()));
  }

  private static LinkedRequest toLinkedRequest(Record r) {
    var consent =
        new GuardianConsent(
            r.get("id", UUID.class),
            r.get("user_id", UUID.class),
            PersonName.of(r.get("guardian_name", String.class)),
            GuardianRelationship.fromCode(r.get("relationship", String.class)),
            r.get("doc_version", String.class),
            instant(r.get("granted_at", OffsetDateTime.class)),
            instant(r.get("token_expires_at", OffsetDateTime.class)),
            null,
            null,
            null);
    return new LinkedRequest(consent, r.get("minor_first_name", String.class));
  }

  private static OffsetDateTime utc(Instant instant) {
    return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
  }

  private static Instant instant(OffsetDateTime value) {
    return value == null ? null : value.toInstant();
  }
}
