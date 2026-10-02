package br.com.moveup.db;

import static br.com.moveup.db.DbFixtures.activeLink;
import static br.com.moveup.db.DbFixtures.assertSqlState;
import static br.com.moveup.db.DbFixtures.client;
import static br.com.moveup.db.DbFixtures.invite;
import static br.com.moveup.db.DbFixtures.organization;
import static br.com.moveup.db.DbFixtures.pendingLink;
import static br.com.moveup.db.DbFixtures.single;
import static br.com.moveup.db.DbFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Limite de alunos ativos do plano no aceite de convite (cenário 12 do antigo {@code
 * scenarios.sql}), inclusive sob aceites simultâneos (trava da assinatura em {@code
 * accept_invite}).
 */
class PlanLimitTest {

  @BeforeAll
  static void startDatabase() {
    PostgresTestDatabase.start();
  }

  @Test
  void aceiteAlemDoLimiteDoPlanoFalhaComP0003() throws SQLException {
    // given: plano com 1 aluno, já ocupado
    var pro = user();
    var org = organization(pro, 1);
    activeLink(org, pro, client(pro));
    var code = invite(pendingLink(org, pro, client(pro)));

    // when / then
    assertSqlState("P0003", () -> InviteAcceptanceTest.accept(user(), code));
  }

  @Test
  void aceitesSimultaneosNaoPassamDoLimite() throws Exception {
    // given: plano com 1 aluno e dois convites pendentes
    var pro = user();
    var org = organization(pro, 1);
    var codes =
        new String[] {
          invite(pendingLink(org, pro, client(pro))), invite(pendingLink(org, pro, client(pro)))
        };
    var students = new UUID[] {user(), user()};
    var start = new CountDownLatch(1);

    // when
    var results = new ArrayList<Future<String>>();
    try (var pool = Executors.newFixedThreadPool(2)) {
      for (int i = 0; i < 2; i++) {
        var student = students[i];
        var code = codes[i];
        Callable<String> acceptance =
            () -> {
              start.await();
              try {
                InviteAcceptanceTest.accept(student, code);
                return "ok";
              } catch (SQLException e) {
                return e.getSQLState();
              }
            };
        results.add(pool.submit(acceptance));
      }
      start.countDown();
    }

    // then
    var outcomes = new ArrayList<String>();
    for (var result : results) {
      outcomes.add(get(result));
    }
    assertThat(outcomes).containsExactlyInAnyOrder("ok", "P0003");
    assertThat(
            single(
                "select count(*) from coaching_link where organization_id = ? and status ="
                    + " 'active'",
                org))
        .isEqualTo(1L);
  }

  private static String get(Future<String> future) throws InterruptedException {
    try {
      return future.get();
    } catch (ExecutionException e) {
      throw new AssertionError(e.getCause());
    }
  }
}
