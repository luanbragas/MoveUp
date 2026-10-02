package br.com.moveup.shared.infrastructure.persistence;

import static br.com.moveup.db.DbFixtures.activeLink;
import static br.com.moveup.db.DbFixtures.clientOf;
import static br.com.moveup.db.DbFixtures.organization;
import static br.com.moveup.db.DbFixtures.session;
import static br.com.moveup.db.DbFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.moveup.db.PostgresTestDatabase;
import br.com.moveup.shared.infrastructure.security.CurrentAppUser;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Toda transação recebe o {@code app.user_id} do usuário atual, e o valor não vaza para a próxima
 * transação na mesma conexão (o que o PgBouncer em modo transaction faz o tempo todo).
 */
class RlsTransactionManagerTest {

  private final AtomicReference<UUID> current = new AtomicReference<>();
  private final CurrentAppUser currentAppUser = () -> Optional.ofNullable(current.get());

  private SingleConnectionDataSource dataSource;
  private JdbcTemplate jdbc;
  private TransactionTemplate tx;

  @BeforeAll
  static void startDatabase() {
    PostgresTestDatabase.start();
  }

  @BeforeEach
  void setUp() {
    // uma única conexão física, reaproveitada por todas as transações do teste
    dataSource =
        new SingleConnectionDataSource(
            PostgresTestDatabase.jdbcUrl(),
            PostgresTestDatabase.superuserUser(),
            PostgresTestDatabase.superuserPassword(),
            true);
    jdbc = new JdbcTemplate(dataSource);
    tx = new TransactionTemplate(new RlsTransactionManager(dataSource, currentAppUser));
  }

  @AfterEach
  void tearDown() {
    dataSource.destroy();
  }

  @Test
  void transacaoRecebeOUsuarioAtual() {
    var user = UUID.randomUUID();
    current.set(user);

    String seen = tx.execute(s -> appUserId());

    assertThat(seen).isEqualTo(user.toString());
  }

  @Test
  void semUsuarioATransacaoFicaSemUsuario() {
    current.set(null);

    String seen = tx.execute(s -> appUserId());

    assertThat(seen).isEmpty();
  }

  @Test
  void usuarioNaoVazaParaForaNemParaAProximaTransacao() {
    current.set(UUID.randomUUID());
    tx.executeWithoutResult(s -> appUserId());

    assertThat(appUserId()).as("fora de transação").isNullOrEmpty();

    current.set(null);
    String next = tx.execute(s -> appUserId());
    assertThat(next).as("próxima transação sem usuário").isEmpty();
  }

  @Test
  void rlsUsaOUsuarioDefinidoPeloGerenciador() throws SQLException {
    // given
    var pro = user();
    var stranger = user();
    var student = user();
    var client = clientOf(student, pro);
    var session = session(client, activeLink(organization(pro, 10), pro, client), student);

    // when / then
    current.set(pro);
    assertThat(sessionsVisibleAsAppApi(session)).isEqualTo(1);
    current.set(stranger);
    assertThat(sessionsVisibleAsAppApi(session)).isZero();
  }

  private String appUserId() {
    return jdbc.queryForObject("select current_setting('app.user_id', true)", String.class);
  }

  private Long sessionsVisibleAsAppApi(UUID session) {
    return tx.execute(
        s -> {
          jdbc.execute("set local role app_api");
          return jdbc.queryForObject(
              "select count(*) from workout_session where id = ?", Long.class, session);
        });
  }
}
