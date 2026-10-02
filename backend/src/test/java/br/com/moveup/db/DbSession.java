package br.com.moveup.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Executa um bloco numa transação como um papel da aplicação, do mesmo jeito que a infraestrutura
 * fará em produção: {@code set_config('app.user_id', ?, true)} com bind, válido só na transação.
 * {@link #as} sempre termina em ROLLBACK, então um teste não contamina o outro; {@link #committed}
 * faz COMMIT, para cenários em que o efeito precisa ser lido depois (ex.: aceite de convite).
 */
public final class DbSession {

  /** Papéis de banco da ARQUITETURA.md, seção 5. */
  public enum Role {
    APP_API("app_api"),
    APP_WORKER("app_worker"),
    APP_REPORT("app_report");

    private final String sqlName;

    Role(String sqlName) {
      this.sqlName = sqlName;
    }
  }

  @FunctionalInterface
  public interface Work<T> {
    T run(Connection connection) throws SQLException;
  }

  private DbSession() {}

  public static <T> T as(Role role, UUID userId, Work<T> work) throws SQLException {
    return run(role, userId, work, false);
  }

  /** Como {@link #as}, mas faz COMMIT se o bloco terminar sem erro. */
  public static <T> T committed(Role role, UUID userId, Work<T> work) throws SQLException {
    return run(role, userId, work, true);
  }

  private static <T> T run(Role role, UUID userId, Work<T> work, boolean commit)
      throws SQLException {
    try (Connection c = PostgresTestDatabase.superuser()) {
      c.setAutoCommit(false);
      var succeeded = false;
      try {
        try (var st = c.createStatement()) {
          st.execute("set local role " + role.sqlName); // nome vem do enum, nunca de entrada
        }
        if (userId != null) {
          try (var ps = c.prepareStatement("select set_config('app.user_id', ?, true)")) {
            ps.setString(1, userId.toString());
            ps.execute();
          }
        }
        var result = work.run(c);
        succeeded = true;
        return result;
      } finally {
        if (commit && succeeded) {
          c.commit();
        } else {
          c.rollback();
        }
      }
    }
  }

  public static <T> T asWithoutUser(Role role, Work<T> work) throws SQLException {
    return as(role, null, work);
  }
}
