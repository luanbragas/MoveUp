package br.com.moveup.accounts.infrastructure.web;

import br.com.moveup.accounts.application.port.in.ManageConsents;
import br.com.moveup.accounts.application.port.in.ManageGuardianRequest;
import br.com.moveup.accounts.domain.exception.AccountNotRegistered;
import br.com.moveup.shared.infrastructure.security.CurrentAppUser;
import br.com.moveup.shared.infrastructure.web.ApiProblem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Consentimentos LGPD do próprio usuário e o do responsável pelo menor (ARQUITETURA 7.8). */
@RestController
@Tag(name = "consents", description = "Consentimentos do usuário (LGPD)")
class ConsentController {

  private final ManageConsents manageConsents;
  private final ManageGuardianRequest guardianRequest;
  private final CurrentAppUser currentAppUser;

  ConsentController(
      ManageConsents manageConsents,
      ManageGuardianRequest guardianRequest,
      CurrentAppUser currentAppUser) {
    this.manageConsents = manageConsents;
    this.guardianRequest = guardianRequest;
    this.currentAppUser = currentAppUser;
  }

  @Schema(name = "LegalVersions")
  record LegalVersionsResponse(
      @Schema(
              requiredMode = Schema.RequiredMode.REQUIRED,
              description = "Versão vigente por tipo (terms, privacy, health_data, photos)")
          Map<String, String> consents,
      @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String guardianConsent) {}

  @Schema(name = "ConsentGrant")
  record GrantItem(
      @Schema(
              requiredMode = Schema.RequiredMode.REQUIRED,
              allowableValues = {"terms", "privacy", "health_data", "photos"})
          @NotBlank
          String kind,
      @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max = 50)
          String docVersion) {}

  @Schema(name = "GrantConsents")
  record GrantConsentsRequest(
      @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotEmpty @Size(max = 4)
          List<@Valid GrantItem> grants) {}

  @Schema(name = "GuardianRequest")
  record GuardianRequestBody(
      @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max = 200)
          String guardianName,
      @Schema(
              requiredMode = Schema.RequiredMode.REQUIRED,
              allowableValues = {"mother", "father", "legal_guardian", "other"})
          @NotBlank
          String relationship,
      @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max = 50)
          String docVersion) {}

  @Schema(name = "GuardianLink")
  record GuardianLinkResponse(
      @Schema(
              requiredMode = Schema.RequiredMode.REQUIRED,
              description =
                  "Link para o responsável abrir no celular dele. Só aparece nesta resposta: o"
                      + " app compartilha na hora")
          String url,
      @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant expiresAt) {

    static GuardianLinkResponse from(ManageGuardianRequest.GuardianLink link) {
      return new GuardianLinkResponse(link.url(), link.expiresAt());
    }
  }

  @GetMapping(path = "/v1/legal-documents", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      operationId = "getLegalVersions",
      summary = "Versões vigentes dos textos legais",
      description = "O app mostra estes textos e manda a versão aceita em POST /v1/consents.")
  @ApiResponse(responseCode = "200", description = "Versões vigentes")
  LegalVersionsResponse legalVersions() {
    var view = manageConsents.currentVersions();
    return new LegalVersionsResponse(view.consents(), view.guardianConsent());
  }

  @PostMapping(path = "/v1/consents", consumes = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      operationId = "grantConsents",
      summary = "Registra aceites",
      description = "Cada aceite guarda a versão do texto, o IP e o app (prova do consentimento).")
  @ApiResponse(responseCode = "204", description = "Aceites registrados")
  @ApiResponse(
      responseCode = "409",
      description = "Versão do texto desatualizada (`consent-version-outdated`)",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
              schema = @Schema(implementation = ApiProblem.class)))
  @ApiResponse(
      responseCode = "404",
      description = "Login sem cadastro (`account-not-registered`)",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
              schema = @Schema(implementation = ApiProblem.class)))
  void grant(@Valid @RequestBody GrantConsentsRequest request, HttpServletRequest http) {
    manageConsents.grant(
        currentUser(),
        request.grants().stream()
            .map(g -> new ManageConsents.Grant(g.kind(), g.docVersion()))
            .toList(),
        RequestOrigins.of(http));
  }

  @DeleteMapping("/v1/consents/{kind}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      operationId = "revokeConsent",
      summary = "Revoga um aceite",
      description = "Direito do titular (LGPD). O histórico do aceite é mantido.")
  @ApiResponse(responseCode = "204", description = "Revogado (ou já não havia aceite vigente)")
  void revoke(@PathVariable String kind, HttpServletRequest http) {
    manageConsents.revoke(currentUser(), kind, RequestOrigins.of(http));
  }

  @PostMapping(
      path = "/v1/guardian-consent",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      operationId = "requestGuardianConsent",
      summary = "Menor pede a autorização do responsável",
      description =
          "Obrigatório para menores de 18 anos antes de aceitar convite (LGPD, art. 14). O menor"
              + " indica quem é o responsável e recebe o link para mandar a essa pessoa; só vale"
              + " quando o responsável autoriza pelo link. Os dados do responsável só são visíveis"
              + " para o próprio aluno.")
  @ApiResponse(responseCode = "201", description = "Pedido criado; link para o responsável")
  @ApiResponse(
      responseCode = "409",
      description =
          "`guardian-consent-not-required` (conta de maior), `guardian-consent-already-active`"
              + " (já há pedido aberto ou autorização) ou `consent-version-outdated`",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
              schema = @Schema(implementation = ApiProblem.class)))
  @ApiResponse(
      responseCode = "422",
      description = "`relationship-invalid`, `name-invalid`",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
              schema = @Schema(implementation = ApiProblem.class)))
  GuardianLinkResponse requestGuardian(
      @Valid @RequestBody GuardianRequestBody request, HttpServletRequest http) {
    return GuardianLinkResponse.from(
        guardianRequest.request(
            new ManageGuardianRequest.RequestCommand(
                currentUser(),
                request.guardianName(),
                request.relationship(),
                request.docVersion(),
                RequestOrigins.of(http))));
  }

  @PostMapping(path = "/v1/guardian-consent/link", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      operationId = "resendGuardianLink",
      summary = "Novo link para o responsável",
      description = "Para reenviar o pedido. O link anterior deixa de valer.")
  @ApiResponse(responseCode = "200", description = "Link novo")
  @ApiResponse(
      responseCode = "409",
      description = "`guardian-request-not-pending` (nenhum pedido aguardando o responsável)",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
              schema = @Schema(implementation = ApiProblem.class)))
  GuardianLinkResponse resendGuardianLink() {
    return GuardianLinkResponse.from(guardianRequest.resendLink(currentUser()));
  }

  @DeleteMapping("/v1/guardian-consent")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      operationId = "cancelGuardianRequest",
      summary = "Cancela o pedido ao responsável",
      description = "Para indicar outra pessoa. O link enviado deixa de valer.")
  @ApiResponse(responseCode = "204", description = "Pedido cancelado")
  @ApiResponse(
      responseCode = "409",
      description = "`guardian-request-not-pending` (nenhum pedido aguardando o responsável)",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
              schema = @Schema(implementation = ApiProblem.class)))
  void cancelGuardianRequest() {
    guardianRequest.cancel(currentUser());
  }

  private UUID currentUser() {
    return currentAppUser.id().orElseThrow(AccountNotRegistered::new);
  }
}
