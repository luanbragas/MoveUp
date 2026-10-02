package br.com.moveup.coaching.infrastructure.web;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import br.com.moveup.coaching.application.port.in.InviteClient.Invitation;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(name = "Invitation", description = "Convite para compartilhar (link, código ou QR)")
record InvitationResponse(
    @Schema(requiredMode = REQUIRED) UUID clientId,
    @Schema(requiredMode = REQUIRED) UUID linkId,
    @Schema(requiredMode = REQUIRED, example = "K7M2QX9P") String code,
    @Schema(requiredMode = REQUIRED, example = "https://moveup-site.pages.dev/i/K7M2QX9P")
        String url,
    @Schema(requiredMode = REQUIRED) Instant expiresAt) {

  static InvitationResponse from(Invitation invitation) {
    return new InvitationResponse(
        invitation.clientId(),
        invitation.linkId(),
        invitation.code(),
        invitation.url(),
        invitation.expiresAt());
  }
}
