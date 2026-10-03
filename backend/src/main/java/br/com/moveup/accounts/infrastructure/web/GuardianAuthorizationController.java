package br.com.moveup.accounts.infrastructure.web;

import br.com.moveup.accounts.application.port.in.GuardianAuthorization;
import br.com.moveup.shared.infrastructure.web.ApiProblem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Página do responsável pelo menor (site estático), sem login: quem tem o link decide. O segredo
 * vai no corpo, nunca na URL, para não parar em log de acesso.
 */
@RestController
@Tag(name = "guardian-authorizations", description = "Autorização do responsável pelo menor")
@SecurityRequirements // público: sem Bearer
class GuardianAuthorizationController {

  private final GuardianAuthorization guardianAuthorization;

  GuardianAuthorizationController(GuardianAuthorization guardianAuthorization) {
    this.guardianAuthorization = guardianAuthorization;
  }

  @Schema(name = "GuardianAuthorizationLookup")
  record LookupRequest(
      @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Segredo do link")
          @NotBlank
          @Size(max = 64)
          String token) {}

  @Schema(name = "GuardianAuthorizationPreview")
  record PreviewResponse(
      @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String minorFirstName,
      @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String guardianName,
      @Schema(
              requiredMode = Schema.RequiredMode.REQUIRED,
              allowableValues = {"mother", "father", "legal_guardian", "other"})
          String relationship,
      @Schema(
              requiredMode = Schema.RequiredMode.REQUIRED,
              description = "Versão do termo que a página mostra; volta na decisão")
          String docVersion,
      @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant expiresAt) {}

  @Schema(name = "GuardianAuthorizationDecision")
  record DecisionRequest(
      @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max = 64) String token,
      @Schema(
              requiredMode = Schema.RequiredMode.REQUIRED,
              description = "true = autoriza; false = não autoriza")
          @NotNull
          Boolean approve,
      @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max = 50)
          String docVersion) {}

  @PostMapping(
      path = "/v1/guardian-authorizations/preview",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      operationId = "previewGuardianAuthorization",
      summary = "Pedido de autorização, pelo link",
      description = "Só o primeiro nome do menor e quem ele indicou; nenhum dado de saúde.")
  @ApiResponse(responseCode = "200", description = "Pedido aguardando a decisão")
  @ApiResponse(
      responseCode = "404",
      description = "`guardian-authorization-not-found` (link inválido, vencido ou já usado)",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
              schema = @Schema(implementation = ApiProblem.class)))
  PreviewResponse preview(@Valid @RequestBody LookupRequest request) {
    var preview = guardianAuthorization.preview(request.token());
    return new PreviewResponse(
        preview.minorFirstName(),
        preview.guardianName(),
        preview.relationship(),
        preview.docVersion(),
        preview.expiresAt());
  }

  @PostMapping(
      path = "/v1/guardian-authorizations/decision",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      operationId = "decideGuardianAuthorization",
      summary = "Responsável autoriza ou recusa",
      description =
          "Guarda quando, o IP e o navegador como prova (LGPD). O link vale uma vez: depois da"
              + " decisão responde 404.")
  @ApiResponse(responseCode = "204", description = "Decisão registrada")
  @ApiResponse(
      responseCode = "404",
      description = "`guardian-authorization-not-found` (link inválido, vencido ou já usado)",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
              schema = @Schema(implementation = ApiProblem.class)))
  @ApiResponse(
      responseCode = "409",
      description = "`consent-version-outdated` (o termo mudou: recarregar a página)",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
              schema = @Schema(implementation = ApiProblem.class)))
  void decide(@Valid @RequestBody DecisionRequest request, HttpServletRequest http) {
    guardianAuthorization.decide(
        new GuardianAuthorization.Decision(
            request.token(), request.approve(), request.docVersion(), RequestOrigins.of(http)));
  }
}
