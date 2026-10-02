package br.com.moveup.db;

import static br.com.moveup.db.DbFixtures.client;
import static br.com.moveup.db.DbFixtures.count;
import static br.com.moveup.db.DbFixtures.invite;
import static br.com.moveup.db.DbFixtures.organization;
import static br.com.moveup.db.DbFixtures.pendingLink;
import static br.com.moveup.db.DbFixtures.user;
import static br.com.moveup.db.DbSession.Role.APP_API;
import static br.com.moveup.db.DbSession.Role.APP_REPORT;
import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** RLS do invite (V15): o código é segredo, só o profissional do vínculo o vê. */
class InviteRlsTest {

  private static UUID pro;
  private static UUID stranger;
  private static UUID student;
  private static UUID link;

  @BeforeAll
  static void seed() throws SQLException {
    PostgresTestDatabase.start();
    pro = user();
    stranger = user();
    student = user();
    link = pendingLink(organization(pro, 10), pro, client(pro));
    invite(link);
  }

  @Test
  void soOProfissionalDoVinculoVeOConvite() throws SQLException {
    var sql = "select count(*) from invite where coaching_link_id = ?";

    long byPro = DbSession.as(APP_API, pro, c -> count(c, sql, link));
    long byStranger = DbSession.as(APP_API, stranger, c -> count(c, sql, link));
    long byStudent = DbSession.as(APP_API, student, c -> count(c, sql, link));
    long byReport = DbSession.asWithoutUser(APP_REPORT, c -> count(c, sql, link));

    assertThat(byPro).isEqualTo(1);
    assertThat(byStranger).isZero();
    assertThat(byStudent).isZero();
    assertThat(byReport).isZero();
  }

  @Test
  void previaSoComUsuarioLogadoESemDadoDoAluno() throws SQLException {
    var code =
        (String) DbFixtures.single("select code from invite where coaching_link_id = ?", link);
    var sql = "select count(*) from invite_preview(?)";

    long logged = DbSession.as(APP_API, student, c -> count(c, sql, code));
    long anonymous = DbSession.asWithoutUser(APP_API, c -> count(c, sql, code));

    assertThat(logged).isEqualTo(1);
    assertThat(anonymous).isZero();
  }
}
