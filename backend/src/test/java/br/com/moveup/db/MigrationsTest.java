package br.com.moveup.db;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MigrationsTest {

  @BeforeAll
  static void startDatabase() {
    PostgresTestDatabase.start();
  }

  @Test
  void deveAplicarTodasAsMigrationsSemPendencias() {
    // given
    var flyway =
        Flyway.configure()
            .dataSource(
                PostgresTestDatabase.jdbcUrl(),
                PostgresTestDatabase.ownerUser(),
                PostgresTestDatabase.ownerPassword())
            .load();

    // when
    var info = flyway.info();

    // then
    assertThat(info.pending()).isEmpty();
    assertThat(info.current().getVersion().getVersion()).isEqualTo("21");
  }

  @Test
  void deveTerTombstoneNasTabelasQueOAppBaixa() throws Exception {
    // given
    var sql =
        """
        select table_name from information_schema.columns
         where table_schema = 'public' and column_name = 'deleted_at'
           and table_name in ('program', 'workout', 'health_restriction')
        """;

    // when
    var tables = new java.util.ArrayList<String>();
    try (var c = PostgresTestDatabase.superuser();
        var rs = c.createStatement().executeQuery(sql)) {
      while (rs.next()) {
        tables.add(rs.getString(1));
      }
    }

    // then
    assertThat(tables).containsExactlyInAnyOrder("program", "workout", "health_restriction");
  }
}
