package br.com.moveup.db;

import static br.com.moveup.db.DbFixtures.activeLink;
import static br.com.moveup.db.DbFixtures.assertSqlState;
import static br.com.moveup.db.DbFixtures.client;
import static br.com.moveup.db.DbFixtures.clientOf;
import static br.com.moveup.db.DbFixtures.exec;
import static br.com.moveup.db.DbFixtures.exercise;
import static br.com.moveup.db.DbFixtures.organization;
import static br.com.moveup.db.DbFixtures.session;
import static br.com.moveup.db.DbFixtures.user;

import java.sql.SQLException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** FKs compostas que o RLS pressupõe (cenários 7 e 18 do antigo {@code scenarios.sql}). */
class CompositeFkTest {

  private static final String FOREIGN_KEY_VIOLATION = "23503";

  @BeforeAll
  static void startDatabase() {
    PostgresTestDatabase.start();
  }

  @Test
  void exercicioRealizadoComClientIdDiferenteDaSessaoEhRejeitado() throws SQLException {
    // given
    var pro = user();
    var student = user();
    var client = clientOf(student, pro);
    var session = session(client, activeLink(organization(pro, 10), pro, client), student);
    var otherClient = client(pro);
    var exercise = exercise();

    // when / then
    assertSqlState(
        FOREIGN_KEY_VIOLATION,
        () ->
            exec(
                "insert into performed_exercise(id, session_id, client_id, exercise_id, position,"
                    + " status, client_updated_at) values (?, ?, ?, ?, 1, 'done', now())",
                UUID.randomUUID(),
                session,
                otherClient,
                exercise));
  }

  @Test
  void versaoAtualDeOutroTreinoFalhaNoCommit() throws SQLException {
    // given
    var pro = user();
    var org = organization(pro, 10);
    var workout = UUID.randomUUID();
    var otherWorkout = UUID.randomUUID();
    var otherVersion = UUID.randomUUID();

    // when / then: a FK é deferrable, então o erro só aparece no COMMIT
    assertSqlState(
        FOREIGN_KEY_VIOLATION,
        () -> {
          try (var c = PostgresTestDatabase.superuser()) {
            c.setAutoCommit(false);
            try {
              exec(
                  c,
                  "insert into workout(id, organization_id, is_template, name)"
                      + " values (?, ?, true, 'T1'), (?, ?, true, 'T2')",
                  workout,
                  org,
                  otherWorkout,
                  org);
              exec(
                  c,
                  "insert into workout_version(id, workout_id, version_number, created_by)"
                      + " values (?, ?, 1, ?)",
                  otherVersion,
                  otherWorkout,
                  pro);
              exec(
                  c,
                  "update workout set current_version_id = ? where id = ?",
                  otherVersion,
                  workout);
              c.commit();
            } finally {
              c.rollback();
            }
          }
        });
  }
}
