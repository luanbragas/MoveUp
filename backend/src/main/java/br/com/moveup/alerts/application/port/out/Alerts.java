package br.com.moveup.alerts.application.port.out;

import br.com.moveup.alerts.domain.model.NewAlert;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Alertas gravados (API com RLS do profissional; worker como app_worker). */
public interface Alerts {

  /** Cria se não houver um aberto/adiado com a mesma chave; devolve o id do criado. */
  Optional<UUID> openIfNew(NewAlert alert);

  /** O assunto deixou de valer (ex.: o aluno voltou a treinar): resolve o aberto, se houver. */
  void resolveOpen(UUID professionalId, String dedupeKey, Instant now);

  /**
   * @param status {@code open} (inclui adiados vencidos), {@code snoozed} ou {@code resolved}
   * @param before cursor: só alertas com id menor (mais antigos)
   */
  List<AlertView> list(UUID professionalId, String status, UUID before, int limit, Instant now);

  Optional<AlertView> find(UUID professionalId, UUID alertId);

  void resolve(UUID alertId, Instant now);

  void snooze(UUID alertId, Instant until);

  /** Adiados que venceram voltam a abertos (job diário). */
  int wakeSnoozed(Instant now);

  /** Contagem de abertos (badge da aba). */
  int countOpen(UUID professionalId, Instant now);

  record AlertView(
      UUID id,
      String type,
      String severity,
      String status,
      UUID clientId,
      Map<String, Integer> facts,
      Instant createdAt,
      Instant snoozedUntil,
      Instant resolvedAt) {}
}
