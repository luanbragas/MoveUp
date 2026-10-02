package br.com.moveup.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

/**
 * Semeia dados fictícios como superusuário (fora do RLS), com ids aleatórios: o banco de teste é
 * compartilhado entre as classes, então nenhum teste depende de valor fixo.
 */
public final class DbFixtures {

  private DbFixtures() {}

  public static UUID user() throws SQLException {
    var id = UUID.randomUUID();
    exec(
        "insert into app_user(id, name, email) values (?, 'Usuário Fictício', ?)",
        id,
        id + "@example.test");
    return id;
  }

  /** Organização do profissional, com ele como dono e assinatura em teste no limite dado. */
  public static UUID organization(UUID owner, int maxActiveClients) throws SQLException {
    var org = UUID.randomUUID();
    var plan = UUID.randomUUID();
    exec(
        "insert into organization(id, name, owner_user_id) values (?, 'Org Fictícia', ?)",
        org,
        owner);
    exec(
        "insert into organization_member(organization_id, user_id, role) values (?, ?, 'owner')",
        org,
        owner);
    exec(
        "insert into plan(id, code, name, max_active_clients, price_cents, billing_interval)"
            + " values (?, ?, 'Plano de teste', ?, 0, 'month')",
        plan,
        "test-" + plan,
        maxActiveClients);
    exec(
        "insert into subscription(organization_id, plan_id, status, trial_ends_at)"
            + " values (?, ?, 'trialing', now() + interval '14 days')",
        org,
        plan);
    return org;
  }

  /** Pré-cadastro (ainda sem usuário). */
  public static UUID client(UUID createdBy) throws SQLException {
    var id = UUID.randomUUID();
    exec("insert into client(id, name, created_by) values (?, 'Aluno Fictício', ?)", id, createdBy);
    return id;
  }

  /** Cadastro de aluno já ligado a um usuário (convite aceito). */
  public static UUID clientOf(UUID user, UUID createdBy) throws SQLException {
    var id = UUID.randomUUID();
    exec(
        "insert into client(id, user_id, name, created_by) values (?, ?, 'Aluno Fictício', ?)",
        id,
        user,
        createdBy);
    return id;
  }

  public static UUID pendingLink(UUID org, UUID professional, UUID client) throws SQLException {
    var id = UUID.randomUUID();
    exec(
        "insert into coaching_link(id, organization_id, professional_id, client_id, status)"
            + " values (?, ?, ?, ?, 'pending')",
        id,
        org,
        professional,
        client);
    return id;
  }

  public static UUID activeLink(UUID org, UUID professional, UUID client) throws SQLException {
    var id = UUID.randomUUID();
    exec(
        "insert into coaching_link(id, organization_id, professional_id, client_id, status,"
            + " started_at) values (?, ?, ?, ?, 'active', now())",
        id,
        org,
        professional,
        client);
    return id;
  }

  public static void endLink(UUID link) throws SQLException {
    exec("update coaching_link set status = 'ended', ended_at = now() where id = ?", link);
  }

  /** Convite válido por 7 dias; devolve o código. */
  public static String invite(UUID link) throws SQLException {
    var code = "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    exec(
        "insert into invite(coaching_link_id, code, expires_at) values (?, ?, now() + interval"
            + " '7 days')",
        link,
        code);
    return code;
  }

  public static String expiredInvite(UUID link) throws SQLException {
    var code = "X" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    exec(
        "insert into invite(coaching_link_id, code, expires_at) values (?, ?, now() - interval"
            + " '1 minute')",
        link,
        code);
    return code;
  }

  /** Exercício da biblioteca base, com nome único por execução. */
  public static UUID exercise() throws SQLException {
    var id = UUID.randomUUID();
    exec(
        "insert into exercise(id, name, modality, tracking_type) values (?, ?, 'strength',"
            + " 'reps_load')",
        id,
        "Exercício " + id);
    return id;
  }

  /** Sessão em andamento registrada pelo próprio aluno (id gerado "no app"). */
  public static UUID session(UUID client, UUID link, UUID performedByUser) throws SQLException {
    var id = UUID.randomUUID();
    exec(
        "insert into workout_session(id, client_id, coaching_link_id, status, started_at,"
            + " performed_by, performed_by_user, client_updated_at)"
            + " values (?, ?, ?, 'in_progress', now(), 'client', ?, now())",
        id,
        client,
        link,
        performedByUser);
    return id;
  }

  // ---------------------------------------------------------------------------------------------

  public static int exec(String sql, Object... params) throws SQLException {
    try (Connection c = PostgresTestDatabase.superuser()) {
      return exec(c, sql, params);
    }
  }

  public static int exec(Connection c, String sql, Object... params) throws SQLException {
    try (var ps = c.prepareStatement(sql)) {
      for (int i = 0; i < params.length; i++) {
        ps.setObject(i + 1, params[i]);
      }
      return ps.executeUpdate();
    }
  }

  /** Primeira coluna da primeira linha (a consulta deve devolver exatamente uma linha). */
  public static Object single(Connection c, String sql, Object... params) throws SQLException {
    try (var ps = c.prepareStatement(sql)) {
      for (int i = 0; i < params.length; i++) {
        ps.setObject(i + 1, params[i]);
      }
      try (var rs = ps.executeQuery()) {
        assertThat(rs.next()).as("consulta sem linhas: %s", sql).isTrue();
        return rs.getObject(1);
      }
    }
  }

  public static Object single(String sql, Object... params) throws SQLException {
    try (Connection c = PostgresTestDatabase.superuser()) {
      return single(c, sql, params);
    }
  }

  public static long count(Connection c, String sql, Object... params) throws SQLException {
    return ((Number) single(c, sql, params)).longValue();
  }

  /** Falha se o bloco não lançar {@link SQLException} com o SQLSTATE esperado. */
  public static void assertSqlState(String expected, ThrowingCallable call) {
    var thrown = catchThrowable(call);
    assertThat(thrown).isInstanceOf(SQLException.class);
    assertThat(((SQLException) thrown).getSQLState()).isEqualTo(expected);
  }
}
