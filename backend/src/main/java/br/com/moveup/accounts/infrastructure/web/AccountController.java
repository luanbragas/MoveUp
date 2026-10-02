package br.com.moveup.accounts.infrastructure.web;

import br.com.moveup.accounts.application.port.in.GetMe;
import br.com.moveup.accounts.application.port.in.RegisterAccount;
import br.com.moveup.accounts.domain.model.LoginIdentity;
import br.com.moveup.shared.infrastructure.security.FirebaseJwt;
import br.com.moveup.shared.infrastructure.web.ApiProblem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "accounts", description = "Conta do usuário autenticado")
class AccountController {

  private final RegisterAccount registerAccount;
  private final GetMe getMe;

  AccountController(RegisterAccount registerAccount, GetMe getMe) {
    this.registerAccount = registerAccount;
    this.getMe = getMe;
  }

  @Schema(name = "RegisterAccount")
  record RegisterAccountRequest(
      @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max = 200) String name,
      @Schema(
              requiredMode = Schema.RequiredMode.REQUIRED,
              allowableValues = {"professional", "client"})
          @NotBlank
          @Pattern(regexp = "professional|client")
          String role,
      @Schema(description = "Obrigatória para aluno (decide se precisa do responsável)")
          LocalDate birthDate,
      @Schema(description = "Só profissional; vazio usa o nome da pessoa") @Size(max = 120)
          String businessName,
      @Schema(description = "Só profissional; CREF") @Size(max = 30) String registryNumber) {}

  @PostMapping(
      path = "/v1/accounts",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      operationId = "registerAccount",
      summary = "Cria a conta do usuário logado",
      description =
          "Primeira chamada depois do login no provedor. O e-mail vem do token. Profissional ganha"
              + " organização e período de teste; aluno informa a data de nascimento.")
  @ApiResponse(responseCode = "201", description = "Conta criada")
  @ApiResponse(
      responseCode = "409",
      description = "Login ou e-mail já cadastrado (`account-already-registered`)",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
              schema = @Schema(implementation = ApiProblem.class)))
  @ApiResponse(
      responseCode = "422",
      description =
          "Dado inválido: `email-required`, `name-invalid`, `role-invalid`, `birth-date-required`,"
              + " `birth-date-invalid`, `professional-must-be-adult`, `business-name-invalid`,"
              + " `registry-number-invalid`",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
              schema = @Schema(implementation = ApiProblem.class)))
  MeResponse register(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody RegisterAccountRequest request) {
    var userId =
        registerAccount.handle(
            new RegisterAccount.Command(
                new LoginIdentity(FirebaseJwt.PROVIDER, jwt.getSubject()),
                jwt.getClaimAsString("email"),
                request.name(),
                request.role(),
                request.birthDate(),
                request.businessName(),
                request.registryNumber()));
    return MeResponse.from(getMe.handle(userId));
  }
}
