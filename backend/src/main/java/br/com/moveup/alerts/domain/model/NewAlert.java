package br.com.moveup.alerts.domain.model;

import java.util.Map;
import java.util.UUID;

/**
 * Alerta a criar. A chave de deduplicação garante um aberto por assunto (ex.: {@code
 * inactive:<cliente>}): gerar de novo enquanto houver um aberto não cria outro.
 *
 * @param facts só números que explicam o alerta (dias, %, contagem); nunca texto do aluno ou dado
 *     clínico
 */
public record NewAlert(
    UUID professionalId,
    UUID organizationId,
    UUID clientId,
    AlertType type,
    Map<String, Integer> facts,
    String dedupeKey) {

  public NewAlert {
    facts = Map.copyOf(facts);
  }

  public AlertType.Severity severity() {
    return type.severity();
  }
}
