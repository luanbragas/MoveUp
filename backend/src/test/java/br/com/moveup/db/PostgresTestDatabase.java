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

  private PostgresTestDatabase() {}

  /** Sobe o container (uma vez por JVM) e aplica todas as migrations como dono das tabelas. */
  public static synchronized void start() {
    if (migrated) {
      return;
    }
    CONTAINER.start();
    try (Connection c = superuserConnection(CONTAINER.getDatabaseName());
        Statement st = c.createStatement()) {
      st.execute("create role " + OWNER + " login password '" + OWNER_PASSWORD + "'");
      st.execute("create database " + DATABASE + " owner " + OWNER);
    } catch (SQLException e) {
      throw new IllegalStateException("falha ao preparar o banco de teste", e);
    }
    Flyway.configure()
        .dataSource(jdbcUrl(), OWNER, OWNER_PASSWORD)
        .locations("classpath:db/migration")
        .load()
        .migrate();
    migrated = true;
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
