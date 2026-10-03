package br.com.moveup.alerts.infrastructure.web;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import br.com.moveup.accounts.api.AccountDirectory;
import br.com.moveup.alerts.application.port.in.ManageAlerts;
import br.com.moveup.alerts.domain.model.AlertSetting;
import br.com.moveup.alerts.domain.model.AlertType;
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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Central de atenção (SCREEN-FLOWS: aba Atenção do profissional), configurações e aparelhos. */
@RestController
@Tag(name = "alerts", description = "Central de atenção do profissional e push")
@ApiResponse(
    responseCode = "403",
    description = "Conta não é de profissional (`forbidden`)",
    content =
        @Content(
            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
            schema = @Schema(implementation = ApiProblem.class)))
class AlertsController {

  private final ManageAlerts alerts;
  private final AccountDirectory accounts;
  private final CurrentAppUser currentAppUser;

  AlertsController(ManageAlerts alerts, AccountDirectory accounts, CurrentAppUser currentAppUser) {
    this.alerts = alerts;
    this.accounts = accounts;
    this.currentAppUser = currentAppUser;
  }

  @Schema(name = "AlertItem")
  record AlertResponse(
      @Schema(requiredMode = REQUIRED) UUID id,
      @Schema(
              requiredMode = REQUIRED,
              allowableValues = {
                "pain_reported",
                "high_effort",
                "new_feedback",
                "session_edited",
                "inactive",
                "low_adherence",
                "clearance_pending"
              })
          String type,
      @Schema(
              requiredMode = REQUIRED,
              allowableValues = {"info", "warning", "urgent"})
          String severity,
      @Schema(
              requiredMode = REQUIRED,
              allowableValues = {"open", "snoozed", "resolved"})
          String status,
      @Schema(requiredMode = REQUIRED) UUID clientId,
      @Schema(description = "Vínculo atual do aluno com o profissional") UUID linkId,
      String clientName,
      @Schema(
              requiredMode = REQUIRED,
              description = "Só números: days, percent, planned, done, effort, count")
          Map<String, Integer> facts,
      @Schema(requiredMode = REQUIRED) Instant createdAt,
      Instant snoozedUntil) {}

  @Schema(name = "AlertPage")
  record AlertPageResponse(
      @Schema(requiredMode = REQUIRED) List<AlertResponse> items,
      @Schema(description = "Cursor da próxima página (ausente no fim)") UUID next,
      @Schema(requiredMode = REQUIRED, description = "Abertos no total (badge)") int openCount) {}

  @Schema(name = "AlertSnooze")
  record SnoozeRequest(@Schema(requiredMode = REQUIRED, description = "1 a 30") int days) {}

  @Schema(name = "AlertSetting")
  record SettingDto(
      @Schema(
              requiredMode = REQUIRED,
              allowableValues = {
                "pain_reported",
                "high_effort",
                "new_feedback",
                "session_edited",
                "inactive",
                "low_adherence",
                "clearance_pending"
              })
          @NotBlank
          String type,
      @Schema(requiredMode = REQUIRED) boolean enabled,
      @Schema(requiredMode = REQUIRED) boolean push,
      @Schema(
              description =
                  "inactive: dias sem treinar (2–60); high_effort: esforço (5–10);"
                      + " low_adherence: % mínima (10–100). Ausente nos tipos sem limite")
          Integer threshold) {

    static SettingDto of(AlertSetting s) {
      return new SettingDto(s.type().code(), s.enabled(), s.push(), s.threshold());
    }

    AlertSetting toDomain() {
      return new AlertSetting(AlertType.fromCode(type), enabled, push, threshold);
    }
  }

  @Schema(name = "AlertSettingsUpdate")
  record SettingsRequest(
      @Schema(requiredMode = REQUIRED) @NotNull @Size(max = 20) List<@Valid SettingDto> settings) {}

  @Schema(name = "PushDevice")
  record DeviceRequest(
      @Schema(requiredMode = REQUIRED, description = "ExponentPushToken[...]")
          @NotBlank
          @Size(max = 200)
          String token,
      @Schema(
              requiredMode = REQUIRED,
              allowableValues = {"ios", "android"})
          @NotBlank
          String platform) {}

  @Schema(name = "PushDeviceRemoval")
  record DeviceRemoval(@Schema(requiredMode = REQUIRED) @NotBlank @Size(max = 200) String token) {}

  @GetMapping(path = "/v1/alerts", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      operationId = "listAlerts",
      summary = "Alertas do profissional (mais novos primeiro)",
      description = "status: open (padrão; inclui adiados vencidos), snoozed ou resolved.")
  @ApiResponse(responseCode = "200", description = "Uma página de alertas")
  AlertPageResponse list(
      @RequestParam(required = false) String status,
      @RequestParam(required = false) UUID before,
      @RequestParam(defaultValue = "30") int limit) {
    var page = alerts.list(professional(), status, before, limit);
    return new AlertPageResponse(
        page.items().stream()
            .map(
                a ->
                    new AlertResponse(
                        a.id(),
                        a.type(),
                        a.severity(),
                        a.status(),
                        a.clientId(),
                        a.linkId(),
                        a.clientName(),
                        a.facts(),
                        a.createdAt(),
                        a.snoozedUntil()))
            .toList(),
        page.next(),
        page.openCount());
  }

  @PostMapping("/v1/alerts/{alertId}/resolve")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(operationId = "resolveAlert", summary = "Marca o alerta como resolvido")
  @ApiResponse(responseCode = "204", description = "Resolvido (repetir não muda nada)")
  @ApiResponse(
      responseCode = "404",
      description = "`resource-not-found`",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
              schema = @Schema(implementation = ApiProblem.class)))
  void resolve(@PathVariable UUID alertId) {
    alerts.resolve(professional(), alertId);
  }

  @PostMapping(path = "/v1/alerts/{alertId}/snooze", consumes = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(operationId = "snoozeAlert", summary = "Adia o alerta por alguns dias")
  @ApiResponse(responseCode = "204", description = "Adiado")
  @ApiResponse(
      responseCode = "422",
      description = "`snooze-invalid`, `alert-resolved`",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
              schema = @Schema(implementation = ApiProblem.class)))
  void snooze(@PathVariable UUID alertId, @RequestBody SnoozeRequest request) {
    alerts.snooze(professional(), alertId, request.days());
  }

  @GetMapping(path = "/v1/alert-settings", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(operationId = "getAlertSettings", summary = "Configurações de alerta (com padrões)")
  @ApiResponse(responseCode = "200", description = "Uma por tipo")
  List<SettingDto> settings() {
    return alerts.settings(professional()).stream().map(SettingDto::of).toList();
  }

  @PutMapping(
      path = "/v1/alert-settings",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(operationId = "updateAlertSettings", summary = "Altera configurações de alerta")
  @ApiResponse(responseCode = "200", description = "Todas as configurações depois da mudança")
  @ApiResponse(
      responseCode = "422",
      description = "`threshold-invalid`, `alert-type-invalid`",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
              schema = @Schema(implementation = ApiProblem.class)))
  List<SettingDto> updateSettings(@Valid @RequestBody SettingsRequest request) {
    var changes = request.settings().stream().map(SettingDto::toDomain).toList();
    return alerts.updateSettings(professional(), changes).stream().map(SettingDto::of).toList();
  }

  @PutMapping(path = "/v1/push-devices", consumes = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(operationId = "registerPushDevice", summary = "Registra o aparelho para push")
  @ApiResponse(responseCode = "204", description = "Registrado (repetir atualiza)")
  @ApiResponse(
      responseCode = "422",
      description = "`push-token-invalid`, `platform-invalid`",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
              schema = @Schema(implementation = ApiProblem.class)))
  void registerDevice(@Valid @RequestBody DeviceRequest request) {
    alerts.registerDevice(user(), request.token(), request.platform());
  }

  @PostMapping(path = "/v1/push-devices/remove", consumes = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      operationId = "removePushDevice",
      summary = "Tira o aparelho do push (ao sair da conta)")
  @ApiResponse(responseCode = "204", description = "Removido (ou já não existia)")
  void removeDevice(@Valid @RequestBody DeviceRemoval request) {
    alerts.unregisterDevice(user(), request.token());
  }

  private UUID user() {
    return currentAppUser.id().orElseThrow(Forbidden::new);
  }

  private UUID professional() {
    var user = user();
    if (accounts.organizationOf(user).isEmpty()) {
      throw new Forbidden();
    }
    return user;
  }
}
