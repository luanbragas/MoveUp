package br.com.moveup.accounts.infrastructure.web;

import br.com.moveup.accounts.application.port.in.GetMe;
import br.com.moveup.accounts.domain.exception.AccountNotRegistered;
import br.com.moveup.shared.infrastructure.security.CurrentAppUser;
import br.com.moveup.shared.infrastructure.web.ApiProblem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/me")
@Tag(name = "accounts", description = "Conta do usuário autenticado")
class MeController {

  private final GetMe getMe;
  private final CurrentAppUser currentAppUser;

  MeController(GetMe getMe, CurrentAppUser currentAppUser) {
    this.getMe = getMe;
    this.currentAppUser = currentAppUser;
  }

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      operationId = "getMe",
      summary = "Conta do usuário autenticado",
      description =
          "Devolve a conta ligada ao token. Login válido sem cadastro no MoveUp responde 404 com"
              + " `account-not-registered`: o app leva ao cadastro.")
  @ApiResponse(responseCode = "200", description = "Conta encontrada")
  @ApiResponse(
      responseCode = "401",
      description = "Sem token ou token inválido (`unauthenticated`)",
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
  MeResponse me() {
    var userId = currentAppUser.id().orElseThrow(AccountNotRegistered::new);
    return MeResponse.from(getMe.handle(userId));
  }
}
