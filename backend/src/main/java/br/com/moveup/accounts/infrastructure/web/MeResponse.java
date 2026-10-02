package br.com.moveup.accounts.infrastructure.web;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import br.com.moveup.accounts.application.port.in.MeView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(name = "Me", description = "Conta do usuário autenticado")
record MeResponse(
    @Schema(requiredMode = REQUIRED) UUID id,
    @Schema(requiredMode = REQUIRED) String name,
    @Schema(requiredMode = REQUIRED, format = "email") String email,
    @Schema(requiredMode = REQUIRED, example = "pt-BR") String locale,
    @Schema(requiredMode = REQUIRED, example = "America/Sao_Paulo") String timezone,
    @Schema(
            requiredMode = REQUIRED,
            allowableValues = {"kg", "lb"})
        String weightUnit,
    @Schema(
            requiredMode = REQUIRED,
            allowableValues = {"cm", "in"})
        String lengthUnit,
    @Schema(requiredMode = REQUIRED, description = "Tem perfil de profissional")
        boolean professional) {

  static MeResponse from(MeView view) {
    return new MeResponse(
        view.id(),
        view.name(),
        view.email(),
        view.locale(),
        view.timezone(),
        view.weightUnit(),
        view.lengthUnit(),
        view.professional());
  }
}
