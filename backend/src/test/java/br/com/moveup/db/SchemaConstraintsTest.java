package br.com.moveup.db;

import static br.com.moveup.db.DbFixtures.activeLink;
import static br.com.moveup.db.DbFixtures.assertSqlState;
import static br.com.moveup.db.DbFixtures.client;
import static br.com.moveup.db.DbFixtures.clientOf;
import static br.com.moveup.db.DbFixtures.exec;
import static br.com.moveup.db.DbFixtures.organization;
import static br.com.moveup.db.DbFixtures.session;
import static br.com.moveup.db.DbFixtures.single;
import static br.com.moveup.db.DbFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.SQLException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Regras que vivem no schema: UUIDv7, unicidades, dedupe de alerta, anamnese imutável e {@code
 * updated_at} (cenários 1, 9, 10, 11, 15 e 16 do antigo {@code scenarios.sql}).
 */
class SchemaConstraintsTest {

  private static final String UNIQUE_VIOLATION = "23505";

  @BeforeAll
  static void startDatabase() {
    PostgresTestDatabase.start();
  }

  @Test
  void uuidGeradoNoServidorEhVersao7() throws SQLException {
    var id = (UUID) single("select uuid_generate_v7()");

    assertThat(id.version()).isEqualTo(7);
  }

  @Test
  void medidaDoSistemaComCodigoRepetidoEhRejeitada() throws SQLException {
    // given
    var code = "m-" + UUID.randomUUID();
    exec("insert into measurement_type(code, name) values (?, 'Cintura')", code);

    // when / then
    assertSqlState(
        UNIQUE_VIOLATION,
        () -> exec("insert into measurement_type(code, name) values (?, 'Cintura')", code));
  }

  @Test
  void exercicioRepetidoIgnorandoAcentoECaixaEhRejeitado() throws SQLException {
    // given
    var suffix = UUID.randomUUID().toString();
    exec(
        "insert into exercise(name, modality, tracking_type) values (?, 'strength', 'reps_load')",
        "Elevação lateral " + suffix);

    // when / then
    assertSqlState(
        UNIQUE_VIOLATION,
        () ->
            exec(
                "insert into exercise(name, modality, tracking_type) values (?, 'strength',"
                    + " 'reps_load')",
                "ELEVACAO LATERAL " + suffix.toUpperCase()));
  }

  @Test
  void alertaAbertoDeduplicaPorProfissional() throws SQLException {
    // given: o mesmo aluno com dois profissionais (antes e depois de uma troca)
    var proA = user();
    var proB = user();
    var orgA = organization(proA, 10);
    var orgB = organization(proB, 10);
    var client = client(proA);
    var dedupe = "inactive:" + client;
    var sql =
        "insert into alert(organization_id, professional_id, client_id, type, severity, facts,"
            + " dedupe_key) values (?, ?, ?, 'inactive', 'warning', '{}', ?)";

    // when
    exec(sql, orgA, proA, client, dedupe);
    exec(sql, orgB, proB, client, dedupe);

    // then
    assertSqlState(UNIQUE_VIOLATION, () -> exec(sql, orgA, proA, client, dedupe));
  }

  @Test
  void anamneseRevisadaNaoMudaNemEhApagada() throws SQLException {
    // given
    var pro = user();
    var student = user();
    var client = clientOf(student, pro);
    var template = UUID.randomUUID();
    var anamnesis = UUID.randomUUID();
    exec(
        "insert into anamnesis_template(id, name, version, questions) values (?, ?, 1, '[]')",
        template,
        "Modelo " + template);
    exec(
        "insert into anamnesis(id, client_id, template_id, version_number, answers, filled_by,"
            + " reviewed_by, reviewed_at) values (?, ?, ?, 1, '{}', ?, ?, now())",
        anamnesis,
        client,
        template,
        student,
        pro);

    // when / then
    assertThatThrownBy(() -> exec("update anamnesis set goal = 'x' where id = ?", anamnesis))
        .hasMessageContaining("imutável");
    assertThatThrownBy(() -> exec("delete from anamnesis where id = ?", anamnesis))
        .hasMessageContaining("imutável");
  }

  @Test
  void updatedAtEhAtualizadoAutomaticamente() throws SQLException {
    // given
    var pro = user();
    var student = user();
    var client = clientOf(student, pro);
    var session = session(client, activeLink(organization(pro, 10), pro, client), student);

    // when (outra transação: now() diferente do insert)
    exec(
        "update workout_session set status = 'completed', finished_at = now() where id = ?",
        session);

    // then
    assertThat(single("select updated_at > created_at from workout_session where id = ?", session))
        .isEqualTo(true);
  }
}
