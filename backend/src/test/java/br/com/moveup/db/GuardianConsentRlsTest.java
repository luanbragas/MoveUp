package br.com.moveup.db;

import static br.com.moveup.db.DbFixtures.count;
import static br.com.moveup.db.DbFixtures.exec;
import static br.com.moveup.db.DbFixtures.user;
import static br.com.moveup.db.DbSession.Role.APP_API;
import static br.com.moveup.db.DbSession.Role.APP_REPORT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.SQLException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** RLS do {@code guardian_consent} (V14): dados do responsável só para o próprio menor. */
class GuardianConsentRlsTest {

  private static UUID minor;
  private static UUID other;

  @BeforeAll
  static void seed() throws SQLException {
    PostgresTestDatabase.start();
    minor = user();
    other = user();
    exec(
        "insert into guardian_consent(user_id, guardian_name, guardian_email, relationship,"
            + " doc_version) values (?, 'Maria', 'maria@example.test', 'mother', 'v1')",
        minor);
  }

  @Test
  void soOProprioMenorVeOConsentimento() throws SQLException {
    var sql = "select count(*) from guardian_consent where user_id = ?";

    long byMinor = DbSession.as(APP_API, minor, c -> count(c, sql, minor));
    long byOther = DbSession.as(APP_API, other, c -> count(c, sql, minor));
    long withoutUser = DbSession.asWithoutUser(APP_API, c -> count(c, sql, minor));

    assertThat(byMinor).isEqualTo(1);
    assertThat(byOther).isZero();
    assertThat(withoutUser).isZero();
  }

  @Test
  void ninguemGravaPorOutro() {
    assertThatThrownBy(
            () ->
                DbSession.as(
                    APP_API,
                    other,
                    c ->
                        exec(
                            c,
                            "insert into guardian_consent(user_id, guardian_name, guardian_email,"
                                + " relationship, doc_version) values (?, 'X', 'x@example.test',"
                                + " 'other', 'v1')",
                            minor)))
        .hasMessageContaining("row-level security");
  }

  @Test
  void apiNaoApagaERelatorioNaoLe() {
    assertThatThrownBy(
            () -> DbSession.as(APP_API, minor, c -> exec(c, "delete from guardian_consent")))
        .hasMessageContaining("permission denied");
    assertThatThrownBy(
            () ->
                DbSession.asWithoutUser(
                    APP_REPORT, c -> count(c, "select count(*) from guardian_consent")))
        .hasMessageContaining("permission denied");
  }

  @Test
  void soUmConsentimentoVigentePorMenor() {
    assertThatThrownBy(
            () ->
                exec(
                    "insert into guardian_consent(user_id, guardian_name, guardian_email,"
                        + " relationship, doc_version) values (?, 'Joao', 'joao@example.test',"
                        + " 'father', 'v1')",
                    minor))
        .isInstanceOf(SQLException.class)
        .hasMessageContaining("one_active_guardian_consent");
  }
}
