package br.com.moveup.coaching.infrastructure.web;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import br.com.moveup.coaching.application.port.in.InviteClient;
import br.com.moveup.coaching.application.port.in.ListClients;
import br.com.moveup.coaching.application.port.in.ManageLink;
import br.com.moveup.shared.domain.Forbidden;
import br.com.moveup.shared.infrastructure.security.CurrentAppUser;
import br.com.moveup.shared.infrastructure.web.ApiProblem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Lado do profissional: alunos, convites e ciclo de vida do vínculo (SCREEN-FLOWS 2.2). */
@RestController
@Tag(name = "clients", description = "Alunos e vínculos do profissional")
@ApiResponse(
    responseCode = "403",
    description = "Conta não é de profissional (`forbidden`)",
    content =
        @Content(
            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
            schema = @Schema(implementation = ApiProblem.class)))
class ClientsController {

  private final InviteClient inviteClient;
  private final ListClients listClients;
  private final ManageLink manageLink;
  private final CurrentAppUser currentAppUser;

  ClientsController(
      InviteClient inviteClient,
      ListClients listClients,
      ManageLink manageLink,
      CurrentAppUser currentAppUser) {
    this.inviteClient = inviteClient;
    this.listClients = listClients;
    this.manageLink = manageLink;
    this.currentAppUser = currentAppUser;
  }

  @Schema(name = "NewClient")
  record NewClientRequest(
      @Schema(requiredMode = REQUIRED) @NotBlank @Size(max = 200) String name,
      @Schema(format = "email") @Size(max = 254) String email,
      @Schema(description = "WhatsApp com DDI e DDD", example = "+55 11 98765-4321") @Size(max = 30)
          String phone,
      @Size(max = 500) String goal) {}

  @Schema(name = "ClientPendingInvite")
  record PendingInviteResponse(
      @Schema(requiredMode = REQUIRED) String code,
      @Schema(requiredMode = REQUIRED) Instant expiresAt) {}

  @Schema(name = "ClientItem")
  record ClientItemResponse(
      @Schema(requiredMode = REQUIRED) UUID linkId,
      @Schema(requiredMode = REQUIRED) UUID clientId,
      @Schema(requiredMode = REQUIRED) String name,
      @Schema(
              requiredMode = REQUIRED,
              allowableValues = {"pending", "active", "inactive"})
          String status,
      @Schema(nullable = true) Instant startedAt,
      @Schema(nullable = true, description = "Só para pendentes com convite válido")
          PendingInviteResponse pendingInvite) {}

  @Schema(name = "ClientPage")
  record ClientPageResponse(
      @Schema(requiredMode = REQUIRED) List<ClientItemResponse> items,
      @Schema(nullable = true, description = "Passe em ?cursor= para a próxima página")
          UUID nextCursor) {}

  @PostMapping(
      path = "/v1/clients",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      operationId = "inviteClient",
      summary = "Pré-cadastra um aluno e gera o convite",
      description = "Cria aluno + vínculo pendente + convite (7 dias). Exige vaga no plano.")
  @ApiResponse(responseCode = "201", description = "Convite criado")
  @ApiResponse(
      responseCode = "409",
      description = "Plano sem vaga (`plan-limit-reached`)",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
              schema = @Schema(implementation = ApiProblem.class)))
  @ApiResponse(
      responseCode = "422",
      description = "`name-invalid`, `email-invalid`, `phone-invalid`, `goal-invalid`",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
              schema = @Schema(implementation = ApiProblem.class)))
  InvitationResponse invite(@Valid @RequestBody NewClientRequest request) {
    return InvitationResponse.from(
        inviteClient.handle(
            currentUser(),
            new InviteClient.NewClient(
                request.name(), request.email(), request.phone(), request.goal())));
  }

  @GetMapping(path = "/v1/clients", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      operationId = "listClients",
      summary = "Alunos do profissional",
      description = "Pendentes, ativos e inativos, do mais novo ao mais antigo, por cursor.")
  @ApiResponse(responseCode = "200", description = "Página de alunos")
  ClientPageResponse list(
      @RequestParam(required = false) UUID cursor, @RequestParam(defaultValue = "50") int limit) {
    var page = listClients.handle(currentUser(), cursor, limit);
    return new ClientPageResponse(
        page.items().stream()
            .map(
                i ->
                    new ClientItemResponse(
                        i.linkId(),
                        i.clientId(),
                        i.name(),
                        i.status(),
                        i.startedAt(),
                        i.pendingInvite() == null
                            ? null
                            : new PendingInviteResponse(
                                i.pendingInvite().code(), i.pendingInvite().expiresAt())))
            .toList(),
        page.nextCursor());
  }

  @Schema(name = "ClientSeats")
  record SeatsResponse(
      @Schema(requiredMode = REQUIRED, description = "Alunos ativos (contam no plano)") int active,
      @Schema(nullable = true, description = "Limite do plano; ausente sem assinatura viva")
          Integer limit) {}

  @GetMapping(path = "/v1/clients/seats", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      operationId = "getClientSeats",
      summary = "Vagas do plano",
      description = "Alunos ativos e o limite do plano vivo (\"7 de 10 vagas\").")
  @ApiResponse(responseCode = "200", description = "Vagas")
  SeatsResponse seats() {
    var seats = listClients.seats(currentUser());
    return new SeatsResponse(seats.active(), seats.limit());
  }

  @PostMapping(path = "/v1/clients/{linkId}/invite", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      operationId = "resendInvite",
      summary = "Reenvia o convite",
      description = "Cancela o convite pendente e gera outro (o link anterior deixa de valer).")
  @ApiResponse(responseCode = "200", description = "Convite novo")
  @ApiResponse(
      responseCode = "409",
      description = "Vínculo não está pendente (`link-state-invalid`)",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
              schema = @Schema(implementation = ApiProblem.class)))
  InvitationResponse resend(@PathVariable UUID linkId) {
    return InvitationResponse.from(manageLink.resendInvite(currentUser(), linkId));
  }

  @DeleteMapping("/v1/clients/{linkId}/invite")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(operationId = "cancelInvite", summary = "Cancela o convite pendente")
  @ApiResponse(responseCode = "204", description = "Convite cancelado")
  void cancel(@PathVariable UUID linkId) {
    manageLink.cancelInvite(currentUser(), linkId);
  }

  @PostMapping("/v1/coaching-links/{linkId}/inactivate")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      operationId = "inactivateLink",
      summary = "Inativa o aluno",
      description = "Libera a vaga do plano; o histórico continua visível para reativar.")
  @ApiResponse(responseCode = "204", description = "Inativado")
  void inactivate(@PathVariable UUID linkId) {
    manageLink.inactivate(currentUser(), linkId);
  }

  @PostMapping("/v1/coaching-links/{linkId}/reactivate")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(operationId = "reactivateLink", summary = "Reativa o aluno (exige vaga no plano)")
  @ApiResponse(responseCode = "204", description = "Reativado")
  @ApiResponse(
      responseCode = "409",
      description = "`plan-limit-reached` ou `link-state-invalid`",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
              schema = @Schema(implementation = ApiProblem.class)))
  void reactivate(@PathVariable UUID linkId) {
    manageLink.reactivate(currentUser(), linkId);
  }

  @PostMapping("/v1/coaching-links/{linkId}/end")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(operationId = "endLink", summary = "Encerra o vínculo")
  @ApiResponse(responseCode = "204", description = "Encerrado")
  @ApiResponse(
      responseCode = "404",
      description = "Vínculo de outro profissional ou inexistente (`resource-not-found`)",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
              schema = @Schema(implementation = ApiProblem.class)))
  void end(@PathVariable UUID linkId) {
    manageLink.end(currentUser(), linkId);
  }

  private UUID currentUser() {
    return currentAppUser.id().orElseThrow(Forbidden::new);
  }
}
