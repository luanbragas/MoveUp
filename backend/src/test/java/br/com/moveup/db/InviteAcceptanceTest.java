package br.com.moveup.db;

import static br.com.moveup.db.DbFixtures.activeLink;
import static br.com.moveup.db.DbFixtures.assertSqlState;
import static br.com.moveup.db.DbFixtures.client;
import static br.com.moveup.db.DbFixtures.clientOf;
import static br.com.moveup.db.DbFixtures.endLink;
import static br.com.moveup.db.DbFixtures.exec;
import static br.com.moveup.db.DbFixtures.expiredInvite;
import static br.com.moveup.db.DbFixtures.invite;
import static br.com.moveup.db.DbFixtures.organization;
import static br.com.moveup.db.DbFixtures.pendingLink;
import static br.com.moveup.db.DbFixtures.single;
import static br.com.moveup.db.DbFixtures.user;
import static br.com.moveup.db.DbSession.Role.APP_API;
import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Função {@code accept_invite} (V12): cenários 3 e 13 do antigo {@code scenarios.sql}. */
class InviteAcceptanceTest {

  @BeforeAll
  static void startDatabase() {
    PostgresTestDatabase.start();
  }

  @Test
  void alunoAceitaConviteEVinculoFicaAtivo() throws SQLException {
    // given
    var pro = user();
    var student = user();
    var client = client(pro);
    var link = pendingLink(organization(pro, 10), pro, client);
    var code = invite(link);

    // when
    var accepted = accept(student, code);

    // then
    assertThat(accepted).isEqualTo(link);
    assertThat(single("select status from coaching_link where id = ?", link)).isEqualTo("active");
    assertThat(single("select user_id from client where id = ?", client)).isEqualTo(student);
    assertThat(single("select accepted_by from invite where code = ?", code)).isEqualTo(student);
  }

  @Test
  void conviteExpiradoOuJaUsadoEhRejeitado() throws SQLException {
    // given
    var pro = user();
    var org = organization(pro, 10);
    var expired = expiredInvite(pendingLink(org, pro, client(pro)));
    var used = invite(pendingLink(org, pro, client(pro)));
    accept(user(), used);

    // when / then
    assertSqlState("P0002", () -> accept(user(), expired));
    assertSqlState("P0002", () -> accept(user(), used));
    assertSqlState("P0002", () -> accept(user(), "NAO-EXISTE"));
  }

  @Test
  void aceiteSemUsuarioNaTransacaoFalhaFechado() throws SQLException {
    // given
    var pro = user();
    var code = invite(pendingLink(organization(pro, 10), pro, client(pro)));

    // when / then
    assertSqlState(
        "28000",
        () -> DbSession.asWithoutUser(APP_API, c -> single(c, "select accept_invite(?)", code)));
  }

  @Test
  void trocaDePersonalReaproveitaOCadastroDoAlunoELevaOQueONovoJaLancou() throws SQLException {
    // given: aluno com cadastro e vínculo encerrado com A
    var proA = user();
    var proB = user();
    var student = user();
    var existing = clientOf(student, proA);
    endLink(activeLink(organization(proA, 10), proA, existing));
    // B pré-cadastrou o aluno e já montou um programa nesse pré-cadastro
    var placeholder = client(proB);
    var linkB = pendingLink(organization(proB, 10), proB, placeholder);
    var program = UUID.randomUUID();
    exec(
        "insert into program(id, coaching_link_id, client_id, name, schedule_mode)"
            + " values (?, ?, ?, 'Hipertrofia', 'fixed_days')",
        program,
        linkB,
        placeholder);

    // when
    accept(student, invite(linkB));

    // then
    assertThat(single("select client_id from coaching_link where id = ?", linkB))
        .isEqualTo(existing);
    assertThat(single("select client_id from program where id = ?", program)).isEqualTo(existing);
    assertThat(single("select exists(select 1 from client where id = ?)", placeholder))
        .as("pré-cadastro vazio é removido")
        .isEqualTo(false);
  }

  static UUID accept(UUID user, String code) throws SQLException {
    return DbSession.committed(
        APP_API, user, c -> (UUID) single(c, "select accept_invite(?)", code));
  }
}
