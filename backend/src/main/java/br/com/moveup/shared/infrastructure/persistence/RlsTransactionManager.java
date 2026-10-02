package br.com.moveup.shared.infrastructure.persistence;

import br.com.moveup.shared.infrastructure.security.CurrentAppUser;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.TransactionDefinition;

/**
 * Gerenciador de transações que, no início de <em>toda</em> transação, executa {@code
 * set_config('app.user_id', ?, true)} com bind (CLAUDE.md, regra 4). O terceiro argumento {@code
 * true} limita o valor à transação, o que funciona com PgBouncer em modo transaction e nunca vaza
 * para a próxima requisição que pegar a mesma conexão.
 *
 * <p>Sem usuário, grava string vazia: {@code app_current_user()} vira nulo e as policies de {@code
 * app_api} devolvem zero linhas (falha fechada). Caso de uso nunca chama isto à mão.
 */
public class RlsTransactionManager extends JdbcTransactionManager {

  private static final String SET_USER = "select set_config('app.user_id', ?, true)";

  private final transient CurrentAppUser currentAppUser;

  public RlsTransactionManager(DataSource dataSource, CurrentAppUser currentAppUser) {
    super(dataSource);
    this.currentAppUser = currentAppUser;
  }

  @Override
  protected void prepareTransactionalConnection(Connection con, TransactionDefinition definition)
      throws SQLException {
    super.prepareTransactionalConnection(con, definition);
    try (var ps = con.prepareStatement(SET_USER)) {
      ps.setString(1, currentAppUser.id().map(UUID::toString).orElse(""));
      ps.execute();
    }
  }
}
