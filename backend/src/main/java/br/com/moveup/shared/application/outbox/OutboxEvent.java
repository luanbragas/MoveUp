package br.com.moveup.shared.application.outbox;

import java.util.UUID;

/**
 * Evento lido do outbox. O payload gravado só tem ids (nunca dado de saúde); o handler busca o que
 * precisa a partir do agregado.
 *
 * @param attempts tentativas anteriores que falharam
 */
public record OutboxEvent(long id, String type, UUID aggregateId, int attempts) {}
