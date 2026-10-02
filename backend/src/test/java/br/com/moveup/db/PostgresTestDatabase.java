package br.com.moveup.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Postgres 16 real (Testcontainers), compartilhado pelos testes de banco. Nunca H2.
 *
 * <p>Monta o banco como nos ambientes reais: o banco {@code moveup} pertence a {@code
 * moveup_owner}, que roda o Flyway sem ser superusuário. Os testes conectam como superusuário só
 * para semear dados e, para exercitar o RLS, assumem {@code app_api}/{@code app_worker}/{@code
 * app_report} com {@code SET LOCAL ROLE} dentro da transação (ver {@link DbSession}).
 */
public final class PostgresTestDatabase {

  private static final String OWNER = "moveup_owner";
  private static final String OWNER_PASSWORD = "test-only-owner";
  private static final String DATABASE = "moveup";

  @SuppressWarnings("resource") // vive até o fim da JVM de testes (Ryuk derruba o container)
  private static final PostgreSQLContainer<?> CONTAINER =
      new PostgreSQLContainer<>(
          DockerImageName.parse(
                  System.getProperty("moveup.test.postgres-image", "postgres:16-alpine"))
              .asCompatibleSubstituteFor("postgres"));

  private static volatile boolean migrated;
  private static IllegalStateException setupFailure;

  private PostgresTestDatabase() {}

  /** Sobe o container (uma vez por JVM) e aplica todas as migrations como dono das tabelas. */
  public static synchronized void start() {
    if (migrated) {
      return;
    }
    // Se o preparo falhou para uma classe de teste, as próximas recebem o mesmo erro em vez de
    // tentar recriar papéis que já existem.
    if (setupFailure != null) {
      throw setupFailure;
    }
    try {
      prepareAndMigrate();
      migrated = true;
    } catch (RuntimeException e) {
      setupFailure = new IllegalStateException("falha ao preparar o banco de teste", e);
      throw setupFailure;
    }
  }

  /**
   * Espelha o {@code infra/local/postgres/init}: papéis da aplicação já existem antes da V12, que
   * roda como {@code moveup_owner} (sem permissão de criar papel).
   */
  private static void prepareAndMigrate() {
    CONTAINER.start();
    try (Connection c = superuserConnection(CONTAINER.getDatabaseName());
        Statement st = c.createStatement()) {
      st.execute("create role " + OWNER + " login password '" + OWNER_PASSWORD + "'");
      st.execute("create role app_api nologin nobypassrls");
      st.execute("create role app_worker nologin nobypassrls");
      st.execute("create role app_report nologin nobypassrls");
      st.execute("create database " + DATABASE + " owner " + OWNER);
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    }
    Flyway.configure()
        .dataSource(jdbcUrl(), OWNER, OWNER_PASSWORD)
        .locations("classpath:db/migration")
        .load()
        .migrate();
  }

  public static String jdbcUrl() {
    return "jdbc:postgresql://%s:%d/%s"
        .formatted(CONTAINER.getHost(), CONTAINER.getMappedPort(5432), DATABASE);
  }

  public static String ownerUser() {
    return OWNER;
  }

  public static String ownerPassword() {
    return OWNER_PASSWORD;
  }

  public static String superuserUser() {
    return CONTAINER.getUsername();
  }

  public static String superuserPassword() {
    return CONTAINER.getPassword();
  }

  /** Conexão de superusuário no banco {@code moveup}: só para seed e asserções de catálogo. */
  public static Connection superuser() throws SQLException {
    return superuserConnection(DATABASE);
  }

  private static Connection superuserConnection(String database) throws SQLException {
    var url =
        "jdbc:postgresql://%s:%d/%s"
            .formatted(CONTAINER.getHost(), CONTAINER.getMappedPort(5432), database);
    return DriverManager.getConnection(url, CONTAINER.getUsername(), CONTAINER.getPassword());
  }
}
