package br.com.moveup.accounts.infrastructure.web;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import br.com.moveup.accounts.application.port.in.MeView;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(name = "Me", description = "Conta do usuário autenticado e o que falta no onboarding")
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
    @Schema(
            description = "Papel da conta; ausente se o cadastro não escolheu",
            allowableValues = {"professional", "client"},
            nullable = true)
        String role,
    @Schema(requiredMode = REQUIRED, description = "Menor de 18 anos") boolean minor,
    @ArraySchema(
            arraySchema =
                @Schema(
                    requiredMode = REQUIRED,
                    description =
                        "Consentimentos obrigatórios ainda não aceitos na versão vigente"),
            schema = @Schema(allowableValues = {"terms", "privacy", "health_data", "photos"}))
        List<String> missingConsents,
    @Schema(
            requiredMode = REQUIRED,
            description =
                "Menor sem consentimento do responsável vigente: o app pede antes de seguir")
        boolean guardianConsentRequired) {

  static MeResponse from(MeView view) {
    return new MeResponse(
        view.id(),
        view.name(),
        view.email(),
        view.locale(),
        view.timezone(),
        view.weightUnit(),
        view.lengthUnit(),
        view.role(),
        view.minor(),
        view.missingConsents(),
        view.guardianConsentRequired());
  }
}
