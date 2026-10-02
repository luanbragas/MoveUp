package br.com.moveup.shared.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.moveup.shared.infrastructure.web.PostgresErrorTranslator.Translated;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

class PostgresErrorTranslatorTest {

  @ParameterizedTest
  @CsvSource({
    "P0002, CONFLICT, invite-expired",
    "P0003, CONFLICT, plan-limit-reached",
    "P0004, CONFLICT, client-already-linked",
    "P0001, CONFLICT, state-conflict",
    "23505, CONFLICT, already-exists",
    "23503, UNPROCESSABLE_ENTITY, invalid-reference",
    "28000, NOT_FOUND, resource-not-found",
    "42501, NOT_FOUND, resource-not-found"
  })
  void deveTraduzirSqlStateConhecido(String sqlState, HttpStatus status, String code) {
    var error = new DataIntegrityViolationException("x", new SQLException("x", sqlState));

    assertThat(PostgresErrorTranslator.translate(error)).contains(new Translated(status, code));
  }

  @Test
  void sqlStateDesconhecidoNaoEhTraduzido() {
    var error = new DataIntegrityViolationException("x", new SQLException("x", "XX000"));

    assertThat(PostgresErrorTranslator.translate(error)).isEmpty();
  }

  @Test
  void erroSemSqlExceptionNaoEhTraduzido() {
    assertThat(PostgresErrorTranslator.translate(new IllegalStateException("x"))).isEmpty();
  }
}
