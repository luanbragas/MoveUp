package br.com.moveup.coaching.infrastructure.persistence;

import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.INVITE;

import br.com.moveup.coaching.application.port.in.AnswerInvite.InvitePreview;
import br.com.moveup.coaching.application.port.out.Invites;
import br.com.moveup.coaching.domain.model.InviteCode;
import br.com.moveup.shared.infrastructure.persistence.jooq.Routines;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

/**
 * Convites. Insert e cancelamento passam pelo RLS do invite (só o profissional do vínculo); o aluno
 * usa as funções security definer (accept_invite, invite_preview, end_link_as_client).
 */
@Repository
public class JooqInvites implements Invites {

  private static final SecureRandom RANDOM = new SecureRandom();

  private final DSLContext dsl;

  public JooqInvites(DSLContext dsl) {
    this.dsl = dsl;
  }

  @Override
  public void issue(UUID linkId, InviteCode code, Instant expiresAt) {
    dsl.insertInto(INVITE)
        .set(INVITE.COACHING_LINK_ID, linkId)
        .set(INVITE.CODE, code.value())
        .set(INVITE.EXPIRES_AT, expiresAt.atOffset(ZoneOffset.UTC))
        .execute();
  }

  @Override
  public int revokePending(UUID linkId) {
    return dsl.update(INVITE)
        .set(INVITE.REVOKED_AT, DSL.currentOffsetDateTime())
        .where(INVITE.COACHING_LINK_ID.eq(linkId))
        .and(INVITE.ACCEPTED_AT.isNull())
        .and(INVITE.REVOKED_AT.isNull())
        .execute();
  }

  @Override
  public Optional<InvitePreview> preview(InviteCode code) {
    var preview = Routines.invitePreview(code.value());
    return dsl.selectFrom(preview)
        .fetchOptional(
            r ->
                new InvitePreview(
                    r.getProfessionalName(),
                    r.getOrganizationName(),
                    r.getExpiresAt().toInstant()));
  }

  @Override
  public UUID accept(InviteCode code) {
    return Routines.acceptInvite(dsl.configuration(), code.value());
  }

  @Override
  public void endAsClient(UUID linkId) {
    Routines.endLinkAsClient(dsl.configuration(), linkId);
  }

  @Override
  public InviteCode newCode() {
    return InviteCode.generate(RANDOM::nextInt);
  }
}
