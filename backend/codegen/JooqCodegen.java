import java.nio.file.Path;
import java.sql.DriverManager;
import org.flywaydb.core.Flyway;
import org.jooq.codegen.GenerationTool;
import org.jooq.meta.jaxb.Configuration;
import org.jooq.meta.jaxb.Database;
import org.jooq.meta.jaxb.ForcedType;
import org.jooq.meta.jaxb.Generate;
import org.jooq.meta.jaxb.Generator;
import org.jooq.meta.jaxb.Jdbc;
import org.jooq.meta.jaxb.Target;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Gera o código jOOQ a partir do schema real (roda no build via exec-maven-plugin, fase
 * generate-sources, com o classpath de teste).
 *
 * <p>Monta o banco como nos ambientes reais e nos testes ({@code PostgresTestDatabase}): os papéis
 * da aplicação existem antes da V12 e o Flyway roda como {@code moveup_owner}, sem superusuário.
 *
 * <p>Argumentos: pasta das migrations, pasta de saída, pacote, imagem do Postgres.
 */
public final class JooqCodegen {

  private static final String OWNER = "moveup_owner";
  private static final String OWNER_PASSWORD = "codegen-only-owner";
  private static final String DATABASE = "moveup";

  /** Tabelas que não viram código: histórico do Flyway e partições do audit_log (usa-se o pai). */
  private static final String EXCLUDES = "flyway_schema_history|audit_log_.*";

  public static void main(String[] args) throws Exception {
    if (args.length != 4) {
      throw new IllegalArgumentException(
          "uso: JooqCodegen <migrations> <saída> <pacote> <imagem-postgres>");
    }
    var migrations = Path.of(args[0]).toAbsolutePath();
    var output = Path.of(args[1]).toAbsolutePath();
    var packageName = args[2];
    var image = DockerImageName.parse(args[3]).asCompatibleSubstituteFor("postgres");

    try (var container = new PostgreSQLContainer<>(image)) {
      container.start();
      prepare(container);
      var url =
          "jdbc:postgresql://%s:%d/%s"
              .formatted(container.getHost(), container.getMappedPort(5432), DATABASE);

      Flyway.configure()
          .dataSource(url, OWNER, OWNER_PASSWORD)
          .locations("filesystem:" + migrations)
          .load()
          .migrate();
      var excludes = EXCLUDES + extensionFunctions(url);

      GenerationTool.generate(
          new Configuration()
              .withJdbc(
                  new Jdbc()
                      .withDriver("org.postgresql.Driver")
                      .withUrl(url)
                      .withUser(OWNER)
                      .withPassword(OWNER_PASSWORD))
              .withGenerator(
                  new Generator()
                      .withDatabase(
                          new Database()
                              .withName("org.jooq.meta.postgres.PostgresDatabase")
                              .withInputSchema("public")
                              .withExcludes(excludes)
                              // citext (e-mail) vira String; a comparação sem caixa fica no banco
                              .withForcedTypes(
                                  new ForcedType().withName("CLOB").withIncludeTypes("citext")))
                      .withGenerate(
                          new Generate()
                              .withJavaTimeTypes(true)
                              .withFluentSetters(false)
                              .withPojos(false)
                              .withDaos(false))
                      .withTarget(
                          new Target()
                              .withPackageName(packageName)
                              .withDirectory(output.toString())
                              .withClean(true))));
    }
  }

  /**
   * Funções que pertencem a extensões (pg_trgm, unaccent, citext) e não ao nosso schema; viram
   * {@code |nome1|nome2...} para o regex de exclusão.
   */
  private static String extensionFunctions(String url) throws Exception {
    var sql =
        """
        select distinct p.proname
          from pg_proc p
          join pg_depend d on d.classid = 'pg_proc'::regclass and d.objid = p.oid
         where d.deptype = 'e'
        """;
    var names = new StringBuilder();
    try (var c = DriverManager.getConnection(url, OWNER, OWNER_PASSWORD);
        var rs = c.createStatement().executeQuery(sql)) {
      while (rs.next()) {
        names.append('|').append(java.util.regex.Pattern.quote(rs.getString(1)));
      }
    }
    return names.toString();
  }

  /** Espelha o {@code infra/local/postgres/init}. */
  private static void prepare(PostgreSQLContainer<?> container) throws Exception {
    try (var c =
            DriverManager.getConnection(
                container.getJdbcUrl(), container.getUsername(), container.getPassword());
        var st = c.createStatement()) {
      st.execute("create role " + OWNER + " login password '" + OWNER_PASSWORD + "'");
      st.execute("create role app_api nologin nobypassrls");
      st.execute("create role app_worker nologin nobypassrls");
      st.execute("create role app_report nologin nobypassrls");
      st.execute("create database " + DATABASE + " owner " + OWNER);
    }
  }
}
