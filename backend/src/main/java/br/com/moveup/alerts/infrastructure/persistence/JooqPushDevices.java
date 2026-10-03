package br.com.moveup.alerts.infrastructure.persistence;

import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.PUSH_DEVICE;

import br.com.moveup.alerts.application.port.out.PushDevices;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

/** Aparelhos para push (sem RLS: sempre filtrados pelo usuário autenticado). */
@Repository
public class JooqPushDevices implements PushDevices {

  private final DSLContext dsl;

  public JooqPushDevices(DSLContext dsl) {
    this.dsl = dsl;
  }

  @Override
  public void register(UUID userId, String token, String platform, Instant now) {
    var seen = now.atOffset(ZoneOffset.UTC);
    dsl.insertInto(PUSH_DEVICE)
        .set(PUSH_DEVICE.USER_ID, userId)
        .set(PUSH_DEVICE.TOKEN, token)
        .set(PUSH_DEVICE.PLATFORM, platform)
        .set(PUSH_DEVICE.LAST_SEEN_AT, seen)
        .onConflict(PUSH_DEVICE.TOKEN)
        .doUpdate()
        .set(PUSH_DEVICE.USER_ID, userId)
        .set(PUSH_DEVICE.PLATFORM, platform)
        .set(PUSH_DEVICE.LAST_SEEN_AT, seen)
        .execute();
  }

  @Override
  public void unregister(UUID userId, String token) {
    dsl.deleteFrom(PUSH_DEVICE)
        .where(PUSH_DEVICE.USER_ID.eq(userId))
        .and(PUSH_DEVICE.TOKEN.eq(token))
        .execute();
  }

  @Override
  public List<String> tokensOf(UUID userId) {
    return dsl.select(PUSH_DEVICE.TOKEN)
        .from(PUSH_DEVICE)
        .where(PUSH_DEVICE.USER_ID.eq(userId))
        .fetch(PUSH_DEVICE.TOKEN);
  }

  @Override
  public void removeTokens(Collection<String> tokens) {
    dsl.deleteFrom(PUSH_DEVICE).where(PUSH_DEVICE.TOKEN.in(tokens)).execute();
  }
}
