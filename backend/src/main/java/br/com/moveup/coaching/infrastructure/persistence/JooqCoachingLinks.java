package br.com.moveup.coaching.infrastructure.persistence;

import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.CLIENT;
import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.COACHING_LINK;
import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.INVITE;

import br.com.moveup.coaching.application.port.in.ListClients.ClientItem;
import br.com.moveup.coaching.application.port.in.ListClients.ClientPage;
import br.com.moveup.coaching.application.port.in.ListClients.PendingInvite;
import br.com.moveup.coaching.application.port.out.CoachingLinks;
import br.com.moveup.coaching.domain.model.ClientPreRegistration;
import br.com.moveup.coaching.domain.model.CoachingLink;
import br.com.moveup.coaching.domain.model.LinkStatus;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

/** Vínculos e pré-cadastros. O RLS de client e coaching_link restringe ao usuário da transação. */
@Repository
public class JooqCoachingLinks implements CoachingLinks {

  private final DSLContext dsl;

  public JooqCoachingLinks(DSLContext dsl) {
    this.dsl = dsl;
  }

  @Override
  public Optional<CoachingLink> find(UUID linkId) {
    return dsl.selectFrom(COACHING_LINK)
        .where(COACHING_LINK.ID.eq(linkId))
        .fetchOptional(
            r ->
                new CoachingLink(
                    r.getId(),
                    r.getOrganizationId(),
                    r.getProfessionalId(),
                    r.getClientId(),
                    LinkStatus.fromCode(r.getStatus()),
                    r.getEndedAt() == null ? null : r.getEndedAt().toInstant()));
  }

  @Override
  public void createPending(
      UUID clientId,
      UUID linkId,
      UUID organizationId,
      UUID professionalId,
      ClientPreRegistration client) {
    dsl.insertInto(CLIENT)
        .set(CLIENT.ID, clientId)
        .set(CLIENT.NAME, client.name())
        .set(CLIENT.EMAIL, client.email().orElse(null))
        .set(CLIENT.PHONE, client.phone().orElse(null))
        .set(CLIENT.CREATED_BY, professionalId)
        .execute();
    dsl.insertInto(COACHING_LINK)
        .set(COACHING_LINK.ID, linkId)
        .set(COACHING_LINK.ORGANIZATION_ID, organizationId)
        .set(COACHING_LINK.PROFESSIONAL_ID, professionalId)
        .set(COACHING_LINK.CLIENT_ID, clientId)
        .set(COACHING_LINK.STATUS, LinkStatus.PENDING.code())
        .set(COACHING_LINK.GOAL, client.goal().orElse(null))
        .execute();
  }

  @Override
  public void saveStatus(CoachingLink link) {
    dsl.update(COACHING_LINK)
        .set(COACHING_LINK.STATUS, link.status().code())
        .set(
            COACHING_LINK.ENDED_AT,
            link.endedAt().map(at -> at.atOffset(ZoneOffset.UTC)).orElse(null))
        .where(COACHING_LINK.ID.eq(link.id()))
        .execute();
  }

  @Override
  public int countActive(UUID organizationId) {
    return dsl.fetchCount(
        COACHING_LINK,
        COACHING_LINK.ORGANIZATION_ID.eq(organizationId),
        COACHING_LINK.STATUS.eq(LinkStatus.ACTIVE.code()));
  }

  @Override
  public ClientPage list(UUID professionalId, UUID cursor, int limit) {
    var pending =
        DSL.select(INVITE.CODE, INVITE.EXPIRES_AT)
            .from(INVITE)
            .where(INVITE.COACHING_LINK_ID.eq(COACHING_LINK.ID))
            .and(INVITE.ACCEPTED_AT.isNull())
            .and(INVITE.REVOKED_AT.isNull())
            .and(INVITE.EXPIRES_AT.gt(DSL.currentOffsetDateTime()))
            .orderBy(INVITE.CREATED_AT.desc())
            .limit(1)
            .asTable("pending_invite");
    var code = pending.field(INVITE.CODE);
    var expiresAt = pending.field(INVITE.EXPIRES_AT);

    var condition =
        COACHING_LINK
            .PROFESSIONAL_ID
            .eq(professionalId)
            .and(COACHING_LINK.STATUS.ne(LinkStatus.ENDED.code()));
    if (cursor != null) {
      condition = condition.and(COACHING_LINK.ID.lt(cursor)); // UUIDv7: ordem de criação
    }
    var rows =
        dsl.select(
                COACHING_LINK.ID,
                CLIENT.ID,
                CLIENT.NAME,
                COACHING_LINK.STATUS,
                COACHING_LINK.STARTED_AT,
                code,
                expiresAt)
            .from(COACHING_LINK)
            .join(CLIENT)
            .on(CLIENT.ID.eq(COACHING_LINK.CLIENT_ID))
            .leftJoin(DSL.lateral(pending))
            .on(DSL.trueCondition())
            .where(condition)
            .orderBy(COACHING_LINK.ID.desc())
            .limit(limit + 1)
            .fetch(
                r ->
                    new ClientItem(
                        r.value1(),
                        r.value2(),
                        r.value3(),
                        r.value4(),
                        toInstant(r.value5()),
                        r.value6() == null
                            ? null
                            : new PendingInvite(r.value6(), toInstant(r.value7()))));
    var hasMore = rows.size() > limit;
    var items = hasMore ? rows.subList(0, limit) : rows;
    return new ClientPage(List.copyOf(items), hasMore ? items.getLast().linkId() : null);
  }

  private static Instant toInstant(OffsetDateTime value) {
    return value == null ? null : value.toInstant();
  }
}
