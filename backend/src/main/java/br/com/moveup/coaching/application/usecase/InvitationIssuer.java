package br.com.moveup.coaching.application.usecase;

import br.com.moveup.coaching.application.port.in.InviteClient.Invitation;
import br.com.moveup.coaching.application.port.out.Invites;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

/** Gera e grava o convite de um vínculo pendente, com link e validade. */
public final class InvitationIssuer {

  private final Invites invites;
  private final Clock clock;
  private final Duration ttl;
  private final String linkBaseUrl;

  /**
   * @param linkBaseUrl ex.: {@code https://moveup-site.pages.dev/i/}; o código vai no fim
   */
  public InvitationIssuer(Invites invites, Clock clock, Duration ttl, String linkBaseUrl) {
    if (ttl.isNegative() || ttl.isZero()) {
      throw new IllegalArgumentException("validade do convite precisa ser positiva");
    }
    this.invites = invites;
    this.clock = clock;
    this.ttl = ttl;
    this.linkBaseUrl = linkBaseUrl.endsWith("/") ? linkBaseUrl : linkBaseUrl + "/";
  }

  Invitation issue(UUID clientId, UUID linkId) {
    var code = invites.newCode();
    var expiresAt = clock.instant().plus(ttl);
    invites.issue(linkId, code, expiresAt);
    return new Invitation(clientId, linkId, code.value(), linkBaseUrl + code.value(), expiresAt);
  }
}
