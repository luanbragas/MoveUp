package br.com.moveup.db;

import static br.com.moveup.db.DbFixtures.activeLink;
import static br.com.moveup.db.DbFixtures.clientOf;
import static br.com.moveup.db.DbFixtures.count;
import static br.com.moveup.db.DbFixtures.exec;
import static br.com.moveup.db.DbFixtures.exercise;
import static br.com.moveup.db.DbFixtures.organization;
import static br.com.moveup.db.DbFixtures.session;
import static br.com.moveup.db.DbFixtures.user;
import static br.com.moveup.db.DbSession.Role.APP_API;
import static br.com.moveup.db.DbSession.Role.APP_REPORT;
import static br.com.moveup.db.DbSession.Role.APP_WORKER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.SQLException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Papéis {@code app_worker} e {@code app_report} e o audit_log append-only (cenários 8 e 17 do
 * antigo {@code scenarios.sql}).
 */
class WorkerRoleTest {

  private static UUID client;
  private static UUID session;

  @BeforeAll
  static void seed() throws SQLException {
    PostgresTestDatabase.start();
    var pro = user();
    var student = user();
    client = clientOf(student, pro);
    session = session(client, activeLink(organization(pro, 10), pro, client), student);
  }

  @Test
  void workerGravaRecordeSemUsuarioNaTransacao() throws SQLException {
    // given
    var exercise = exercise();

    // when
    var inserted =
        DbSession.asWithoutUser(
            APP_WORKER,
            c ->
                exec(
                    c,
                    "insert into personal_record(client_id, exercise_id, record_type, value,"
                        + " session_id, achieved_at) values (?, ?, 'max_load', 32, ?, now())",
                    client,
                    exercise,
                    session));

    // then
    assertThat(inserted).isEqualTo(1);
  }

  @Test
  void auditLogEhSoDeAcrescimoParaApiEWorker() {
    for (var role : new DbSession.Role[] {APP_API, APP_WORKER}) {
      assertThatThrownBy(() -> DbSession.asWithoutUser(role, c -> exec(c, "delete from audit_log")))
          .as("delete como %s", role)
          .hasMessageContaining("permission denied");
      assertThatThrownBy(
              () ->
                  DbSession.asWithoutUser(
                      role, c -> exec(c, "update audit_log set action = action")))
          .as("update como %s", role)
          .hasMessageContaining("permission denied");
    }
  }

  @Test
  void relatorioNaoEnxergaDadoDeSaude() throws SQLException {
    // when
    var visible =
        DbSession.asWithoutUser(
            APP_REPORT,
            c -> count(c, "select count(*) from workout_session where id = ?", session));

    // then: tabela com RLS forçado e sem policy para app_report
    assertThat(visible).isZero();
  }
}
