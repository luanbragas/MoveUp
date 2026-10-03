package br.com.moveup.coaching.infrastructure.persistence;

import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.CLIENT;
import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.COACHING_LINK;

import br.com.moveup.coaching.api.CoachingRoster;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record6;
import org.springframework.stereotype.Repository;

/** Leituras de vínculo para outros módulos (o RLS do usuário da transação vale). */
@Repository
public class JooqCoachingRoster implements CoachingRoster {

  private final DSLContext dsl;

  public JooqCoachingRoster(DSLContext dsl) {
    this.dsl = dsl;
  }

  @Override
  public Optional<LinkOwner> owner(UUID linkId) {
    return selectLinks().where(COACHING_LINK.ID.eq(linkId)).fetchOptional(this::toOwner);
  }

  @Override
  public List<LinkOwner> activeLinks() {
    return selectLinks()
        .where(COACHING_LINK.STATUS.eq("active"))
        .orderBy(COACHING_LINK.ID)
        .fetch(this::toOwner);
  }

  @Override
  public Optional<ClientSelf> clientOfUser(UUID userId) {
    return dsl.select(CLIENT.ID)
        .from(CLIENT)
        .where(CLIENT.USER_ID.eq(userId))
        .fetchOptional(CLIENT.ID)
        .map(
            clientId ->
                new ClientSelf(
                    clientId,
                    dsl.select(COACHING_LINK.ID)
                        .from(COACHING_LINK)
                        .where(COACHING_LINK.CLIENT_ID.eq(clientId))
                        .and(COACHING_LINK.STATUS.eq("active"))
                        .fetchOptional(COACHING_LINK.ID)
                        .orElse(null)));
  }

  @Override
  public Map<UUID, ClientRef> clientsOf(UUID professionalId, Collection<UUID> clientIds) {
    if (clientIds.isEmpty()) {
      return Map.of();
    }
    var refs = new LinkedHashMap<UUID, ClientRef>();
    // o mais recente primeiro: o primeiro que aparecer por aluno é o vínculo atual
    dsl.select(CLIENT.ID, COACHING_LINK.ID, CLIENT.NAME)
        .from(COACHING_LINK)
        .join(CLIENT)
        .on(CLIENT.ID.eq(COACHING_LINK.CLIENT_ID))
        .where(COACHING_LINK.PROFESSIONAL_ID.eq(professionalId))
        .and(COACHING_LINK.CLIENT_ID.in(clientIds))
        .orderBy(COACHING_LINK.CREATED_AT.desc())
        .forEach(
            r -> refs.putIfAbsent(r.value1(), new ClientRef(r.value1(), r.value2(), r.value3())));
    return refs;
  }

  private org.jooq.SelectJoinStep<Record6<UUID, UUID, UUID, UUID, String, java.time.OffsetDateTime>>
      selectLinks() {
    return dsl.select(
            COACHING_LINK.ID,
            COACHING_LINK.CLIENT_ID,
            COACHING_LINK.PROFESSIONAL_ID,
            COACHING_LINK.ORGANIZATION_ID,
            COACHING_LINK.STATUS,
            COACHING_LINK.STARTED_AT)
        .from(COACHING_LINK);
  }

  private LinkOwner toOwner(Record6<UUID, UUID, UUID, UUID, String, java.time.OffsetDateTime> r) {
    return new LinkOwner(
        r.value1(),
        r.value2(),
        r.value3(),
        r.value4(),
        r.value5(),
        r.value6() == null ? null : r.value6().toInstant());
  }
}
