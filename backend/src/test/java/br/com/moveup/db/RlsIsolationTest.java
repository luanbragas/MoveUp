package br.com.moveup.db;

import static br.com.moveup.db.DbFixtures.activeLink;
import static br.com.moveup.db.DbFixtures.clientOf;
import static br.com.moveup.db.DbFixtures.count;
import static br.com.moveup.db.DbFixtures.endLink;
import static br.com.moveup.db.DbFixtures.exec;
import static br.com.moveup.db.DbFixtures.organization;
import static br.com.moveup.db.DbFixtures.session;
import static br.com.moveup.db.DbFixtures.user;
import static br.com.moveup.db.DbSession.Role.APP_API;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Isolamento entre profissionais e alunos pelo RLS, conectando como {@code app_api} (cenários 2, 5,
 * 6 e 14 do antigo {@code scenarios.sql}).
 */
class RlsIsolationTest {

  private static final String COUNT_SESSIONS = "select count(*) from workout_session where id = ?";

  @BeforeAll
  static void startDatabase() {
    PostgresTestDatabase.start();
  }

  @Test
  void profissionalPreCadastraAlunoEVinculoPendenteComoAppApi() throws SQLException {
    // given
    var pro = user();
    var org = organization(pro, 10);
    var client = UUID.randomUUID();

    // when
    var links =
        DbSession.as(
            APP_API,
            pro,
            c -> {
              exec(
                  c,
                  "insert into client(id, name, created_by) values (?, 'Aluno Fictício', ?)",
                  client,
                  pro);
              exec(
                  c,
                  "insert into coaching_link(organization_id, professional_id, client_id, status)"
                      + " values (?, ?, ?, 'pending')",
                  org,
                  pro,
                  client);
              return count(c, "select count(*) from coaching_link where client_id = ?", client);
            });

    // then
    assertThat(links).isEqualTo(1);
  }

  @Test
  void sessaoDoAlunoSoEhVistaPeloAlunoEPeloProfissionalDoVinculo() throws SQLException {
    // given
    var proA = user();
    var proB = user();
    var student = user();
    organization(proB, 10);
    var client = clientOf(student, proA);
    var link = activeLink(organization(proA, 10), proA, client);
    var session = session(client, link, student);

    // when / then
    assertThat(sessionsVisibleTo(proA, session)).isEqualTo(1);
    assertThat(sessionsVisibleTo(student, session)).isEqualTo(1);
    assertThat(sessionsVisibleTo(proB, session)).isZero();
    assertThat(sessionsVisibleTo(null, session)).as("sem app.user_id: falha fechada").isZero();
  }

  @Test
  void profissionalSemVinculoNaoGravaSessaoNoAlunoDeOutro() throws SQLException {
    // given
    var proA = user();
    var proB = user();
    var student = user();
    var client = clientOf(student, proA);
    var link = activeLink(organization(proA, 10), proA, client);

    // when / then
    assertThatThrownBy(
            () ->
                DbSession.as(
                    APP_API,
                    proB,
                    c ->
                        exec(
                            c,
                            "insert into workout_session(id, client_id, coaching_link_id, status,"
                                + " started_at, performed_by, performed_by_user,"
                                + " client_updated_at) values (?, ?, ?, 'in_progress', now(),"
                                + " 'professional', ?, now())",
                            UUID.randomUUID(),
                            client,
                            link,
                            proB)))
        .isInstanceOf(SQLException.class)
        .hasMessageContaining("row-level security");
  }

  @Test
  void profissionalComVinculoEncerradoPerdeAcessoEONovoVeOHistorico() throws SQLException {
    // given: o aluno treinou com A, encerrou e passou para B
    var proA = user();
    var proB = user();
    var student = user();
    var client = clientOf(student, proA);
    var linkA = activeLink(organization(proA, 10), proA, client);
    var session = session(client, linkA, student);
    endLink(linkA);
    activeLink(organization(proB, 10), proB, client);

    // when / then (PLANO.md, decisão em aberto "novo personal vê o histórico?": hoje vê)
    assertThat(sessionsVisibleTo(proA, session)).isZero();
    assertThat(sessionsVisibleTo(proB, session)).isEqualTo(1);
  }

  private static long sessionsVisibleTo(UUID user, UUID session) throws SQLException {
    DbSession.Work<Long> work = (Connection c) -> count(c, COUNT_SESSIONS, session);
    return user == null
        ? DbSession.asWithoutUser(APP_API, work)
        : DbSession.as(APP_API, user, work);
  }
}
