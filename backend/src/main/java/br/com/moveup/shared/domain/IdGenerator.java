package br.com.moveup.shared.domain;

import java.util.UUID;

/**
 * Gera ids UUIDv7 (ordenados no tempo) para o que é criado no servidor. Tabelas do offline recebem
 * o id gerado no app.
 */
@FunctionalInterface
public interface IdGenerator {

  UUID newId();
}
