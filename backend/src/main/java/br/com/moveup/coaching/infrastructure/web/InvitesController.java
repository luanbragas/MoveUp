package br.com.moveup.coaching.infrastructure.web;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import br.com.moveup.coaching.application.port.in.AnswerInvite;
import br.com.moveup.coaching.application.port.in.ManageLink;
import br.com.moveup.coaching.application.port.in.MyCoachingLinks;
import br.com.moveup.shared.domain.ResourceNotFound;
import br.com.moveup.shared.infrastructure.security.CurrentAppUser;
import br.com.moveup.shared.infrastructure.web.ApiProblem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Lado do aluno: ver e aceitar o convite, encerrar o próprio vínculo (SCREEN-FLOWS 1.2). */
@RestController
@Tag(name = "invites", description = "Convite do ponto de vista do aluno")
class InvitesController {

  private final AnswerInvite answerInvite;
  private final ManageLink manageLink;
  private final MyCoachingLinks myCoachingLinks;
  private final CurrentAppUser currentAppUser;

  InvitesController(
      AnswerInvite answerInvite,
      ManageLink manageLink,
      MyCoachingLinks myCoachingLinks,
      CurrentAppUser currentAppUser) {
    this.answerInvite = answerInvite;
    this.manageLink = manageLink;
    this.myCoachingLinks = myCoachingLinks;
    this.currentAppUser = currentAppUser;
  }

  @Schema(name = "InvitePreview")
  record InvitePreviewResponse(
      @Schema(requiredMode = REQUIRED, example = "Ana Souza") String professionalName,
      @Schema(requiredMode = REQUIRED, example = "Studio Fit") String organizationName,
      @Schema(requiredMode = REQUIRED) Instant expiresAt) {}

  @Schema(name = "AcceptedInvite")
  record AcceptedInviteResponse(@Schema(requiredMode = REQUIRED) UUID linkId) {}

  @GetMapping(path = "/v1/invites/{code}", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      operationId = "previewInvite",
      summary = "De quem é o convite",
      description = "\"Ana Souza quer ser seu personal\", antes de aceitar.")
  @ApiResponse(responseCode = "200", description = "Convite válido")
  @ApiResponse(
      responseCode = "409",
      description = "Inválido, usado, cancelado ou expirado (`invite-expired`)",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
              schema = @Schema(implementation = ApiProblem.class)))
  InvitePreviewResponse preview(@PathVariable String code) {
    var preview = answerInvite.preview(code);
    return new InvitePreviewResponse(
        preview.professionalName(), preview.organizationName(), preview.expiresAt());
  }

  @PostMapping(path = "/v1/invites/{code}/accept", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      operationId = "acceptInvite",
      summary = "Aceita o convite",
      description =
          "Exige conta de aluno com termos aceitos (e responsável, se menor). Troca de personal:"
              + " o cadastro do aluno é reaproveitado.")
  @ApiResponse(responseCode = "200", description = "Vínculo ativo")
  @ApiResponse(
      responseCode = "409",
      description =
          "`invite-expired`, `plan-limit-reached`, `client-already-linked`,"
              + " `onboarding-incomplete`, `invite-for-clients-only`",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
              schema = @Schema(implementation = ApiProblem.class)))
  AcceptedInviteResponse accept(@PathVariable String code) {
    return new AcceptedInviteResponse(answerInvite.accept(currentUser(), code));
  }

  @Schema(name = "MyCoachingLink")
  record MyLinkResponse(
      @Schema(requiredMode = REQUIRED) UUID linkId,
      @Schema(
              requiredMode = REQUIRED,
              allowableValues = {"pending", "active", "inactive"})
          String status,
      @Schema(nullable = true) Instant startedAt,
      @Schema(requiredMode = REQUIRED, example = "Ana Souza") String professionalName,
      @Schema(requiredMode = REQUIRED, example = "Studio Fit") String organizationName) {}

  @GetMapping(path = "/v1/me/coaching-links", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      operationId = "getMyCoachingLinks",
      summary = "Vínculos do aluno",
      description = "Não encerrados, do mais recente ao mais antigo. Vazio = aluno sem personal.")
  @ApiResponse(responseCode = "200", description = "Vínculos do aluno")
  List<MyLinkResponse> myLinks() {
    return myCoachingLinks.handle(currentUser()).stream()
        .map(
            l ->
                new MyLinkResponse(
                    l.linkId(),
                    l.status(),
                    l.startedAt(),
                    l.professionalName(),
                    l.organizationName()))
        .toList();
  }

  @PostMapping("/v1/me/coaching-links/{linkId}/end")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(operationId = "endMyLink", summary = "O aluno encerra o próprio vínculo")
  @ApiResponse(responseCode = "204", description = "Encerrado")
  @ApiResponse(
      responseCode = "404",
      description = "Vínculo inexistente, de outro aluno ou já encerrado (`resource-not-found`)",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
              schema = @Schema(implementation = ApiProblem.class)))
  void endMine(@PathVariable UUID linkId) {
    manageLink.endAsClient(currentUser(), linkId);
  }

  private UUID currentUser() {
    return currentAppUser.id().orElseThrow(ResourceNotFound::new);
  }
}
