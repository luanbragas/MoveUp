package br.com.moveup.db;

import static br.com.moveup.db.DbSession.Role.APP_API;
import static br.com.moveup.db.DbSession.Role.APP_REPORT;
import static br.com.moveup.db.DbSession.Role.APP_WORKER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * RLS da {@code client_key} (V13). Regra do CLAUDE.md: tabela nova com {@code client_id} = RLS +
 * teste de RLS. Dados fictícios, ids aleatórios por execução (o banco é compartilhado).
 */
class ClientKeyRlsTest {

  private static final UUID PRO_ACTIVE = UUID.randomUUID();
  private static final UUID PRO_ENDED = UUID.randomUUID();
  private static final UUID PRO_STRANGER = UUID.randomUUID();
  private static final UUID STUDENT = UUID.randomUUID();
  private static final UUID CLIENT = UUID.randomUUID();
  private static final byte[] DEK = new byte[32];

  @BeforeAll
  static void seed() throws SQLException {
    PostgresTestDatabase.start();
    var org = UUID.randomUUID();
    try (Connection c = PostgresTestDatabase.superuser()) {
      for (var user : new UUID[] {PRO_ACTIVE, PRO_ENDED, PRO_STRANGER, STUDENT}) {
        exec(
            c,
            "insert into app_user(id, name, email) values (?, 'Fictício', ?)",
            user,
            user + "@example.test");
      }
      exec(
          c,
          "insert into organization(id, name, owner_user_id) values (?, 'Org', ?)",
          org,
          PRO_ACTIVE);
      exec(
          c,
          "insert into client(id, user_id, name, created_by) values (?, ?, 'Aluno Fictício', ?)",
          CLIENT,
          STUDENT,
          PRO_ACTIVE);
      exec(
          c,
          "insert into coaching_link(organization_id, professional_id, client_id, status,"
              + " started_at) values (?, ?, ?, 'active', now())",
          org,
          PRO_ACTIVE,
          CLIENT);
      exec(
          c,
          "insert into coaching_link(organization_id, professional_id, client_id, status,"
              + " started_at, ended_at) values (?, ?, ?, 'ended', now() - interval '30 days', now()"
              + " - interval '1 day')",
          org,
          PRO_ENDED,
          CLIENT);
    }
  }

  @Test
  void profissionalComVinculoAtivoCriaELeAChaveDoAluno() throws SQLException {
    var count =
        DbSession.as(
            APP_API,
            PRO_ACTIVE,
            c -> {
              insertKey(c);
              return countKeys(c);
            });

    assertThat(count).isEqualTo(1);
  }

  @Test
  void alunoLeAPropriaChave() throws SQLException {
    insertKeyCommitted();
    try {
      assertThat(DbSession.as(APP_API, STUDENT, ClientKeyRlsTest::countKeys)).isEqualTo(1);
    } finally {
      deleteKeyAsSuperuser();
    }
  }

  @Test
  void profissionalSemVinculoOuComVinculoEncerradoNaoVeNemCria() throws SQLException {
    insertKeyCommitted();
    try {
      assertThat(DbSession.as(APP_API, PRO_STRANGER, ClientKeyRlsTest::countKeys)).isZero();
      assertThat(DbSession.as(APP_API, PRO_ENDED, ClientKeyRlsTest::countKeys)).isZero();
      assertThat(DbSession.asWithoutUser(APP_API, ClientKeyRlsTest::countKeys)).isZero();
    } finally {
      deleteKeyAsSuperuser();
    }

    assertThatThrownBy(() -> DbSession.as(APP_API, PRO_ENDED, c -> insertKey(c)))
        .isInstanceOf(SQLException.class)
        .hasMessageContaining("row-level security");
  }

  @Test
  void apiNuncaAlteraNemApagaAChave() {
    assertThatThrownBy(
            () ->
                DbSession.as(
                    APP_API,
                    PRO_ACTIVE,
                    c -> c.createStatement().executeUpdate("delete from client_key")))
        .hasMessageContaining("permission denied");
    assertThatThrownBy(
            () ->
                DbSession.as(
                    APP_API,
                    PRO_ACTIVE,
                    c ->
                        c.createStatement()
                            .executeUpdate("update client_key set kms_key_id = 'x'")))
        .hasMessageContaining("permission denied");
  }

  @Test
  void relatorioNaoEnxergaAChave() {
    assertThatThrownBy(() -> DbSession.asWithoutUser(APP_REPORT, ClientKeyRlsTest::countKeys))
        .hasMessageContaining("permission denied");
  }

  @Test
  void workerApagaAChave_cryptoShredding() throws SQLException {
    var deleted =
        DbSession.asWithoutUser(
            APP_WORKER,
            c -> {
              insertKey(c);
              try (var ps = c.prepareStatement("delete from client_key where client_id = ?")) {
                ps.setObject(1, CLIENT);
                return ps.executeUpdate();
              }
            });

    assertThat(deleted).isEqualTo(1);
  }

  // ---------------------------------------------------------------------------------------------

  private static int insertKey(Connection c) throws SQLException {
    try (var ps =
        c.prepareStatement(
            "insert into client_key(client_id, encrypted_dek, kms_key_id) values (?, ?,"
                + " 'test-kms-key')")) {
      ps.setObject(1, CLIENT);
      ps.setBytes(2, DEK);
      return ps.executeUpdate();
    }
  }

  private static int countKeys(Connection c) throws SQLException {
    try (var rs = c.createStatement().executeQuery("select count(*) from client_key")) {
      rs.next();
      return rs.getInt(1);
    }
  }

  private static void insertKeyCommitted() throws SQLException {
    try (Connection c = PostgresTestDatabase.superuser()) {
      exec(
          c,
          "insert into client_key(client_id, encrypted_dek, kms_key_id) values (?, ?,"
              + " 'test-kms-key')",
          CLIENT,
          DEK);
    }
  }

  private static void deleteKeyAsSuperuser() throws SQLException {
    try (Connection c = PostgresTestDatabase.superuser()) {
      exec(c, "delete from client_key where client_id = ?", CLIENT);
    }
  }

  private static void exec(Connection c, String sql, Object... params) throws SQLException {
    try (var ps = c.prepareStatement(sql)) {
      for (int i = 0; i < params.length; i++) {
        ps.setObject(i + 1, params[i]);
      }
      ps.executeUpdate();
    }
  }
}
