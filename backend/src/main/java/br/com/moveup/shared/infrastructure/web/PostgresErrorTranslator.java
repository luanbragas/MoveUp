package br.com.moveup.shared.infrastructure.web;

import java.sql.SQLException;
import java.util.Optional;
import org.springframework.http.HttpStatus;

/**
 * Único lugar que traduz {@code SQLSTATE} do Postgres em erro da API (BACKEND-PATTERN, seção 7).
 * Olha só o código, nunca a mensagem do banco, que pode ter nome de tabela ou valor.
 *
 * <p>Os códigos {@code P000x} vêm das funções das migrations (ex.: {@code accept_invite}).
 */
public final class PostgresErrorTranslator {

  /** Erro traduzido: status HTTP e {@code code} estável. */
  public record Translated(HttpStatus status, String code) {}

  private PostgresErrorTranslator() {}

  public static Optional<Translated> translate(Throwable error) {
    for (var cause = error; cause != null; cause = cause.getCause()) {
      if (cause instanceof SQLException sql && sql.getSQLState() != null) {
        return bySqlState(sql.getSQLState());
      }
    }
    return Optional.empty();
  }

  private static Optional<Translated> bySqlState(String sqlState) {
    return Optional.ofNullable(
        switch (sqlState) {
          case "P0002" -> new Translated(HttpStatus.CONFLICT, "invite-expired");
          case "P0003" -> new Translated(HttpStatus.CONFLICT, "plan-limit-reached");
          case "P0004" -> new Translated(HttpStatus.CONFLICT, "client-already-linked");
          // raise exception sem errcode: estado que não permite a operação (ex.: anamnese revisada)
          case "P0001" -> new Translated(HttpStatus.CONFLICT, "state-conflict");
          case "23505" -> new Translated(HttpStatus.CONFLICT, "already-exists");
          case "23503" -> new Translated(HttpStatus.UNPROCESSABLE_ENTITY, "invalid-reference");
          // sem usuário, convite de outro usuário, RLS ou privilégio: 404 para não revelar nada
          case "28000", "42501" -> new Translated(HttpStatus.NOT_FOUND, "resource-not-found");
          default -> null;
        });
  }
}
