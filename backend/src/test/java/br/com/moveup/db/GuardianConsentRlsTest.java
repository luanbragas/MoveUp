package br.com.moveup.db;

import static br.com.moveup.db.DbFixtures.count;
import static br.com.moveup.db.DbFixtures.exec;
import static br.com.moveup.db.DbFixtures.single;
import static br.com.moveup.db.DbFixtures.user;
import static br.com.moveup.db.DbSession.Role.APP_API;
import static br.com.moveup.db.DbSession.Role.APP_REPORT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * RLS do {@code guardian_consent} (V14): dados do responsável só para o próprio menor. O
 * responsável, sem conta, só chega ao pedido pelas funções da V16, com o hash do segredo do link.
 */
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

  @Test
  void responsavelSoAlcancaOPedidoComOHashCerto() throws SQLException {
    var teen = user();
    var hash = sha256("segredo-" + teen);
    exec(
        "insert into guardian_consent(user_id, guardian_name, relationship, doc_version,"
            + " token_hash, token_expires_at) values (?, 'Maria', 'mother', 'v1', ?,"
            + " now() + interval '7 days')",
        teen,
        hash);
    var lookup = "select count(*) from guardian_request_by_token(?)";

    long withHash = DbSession.asWithoutUser(APP_API, c -> count(c, lookup, hash));
    long withOther = DbSession.asWithoutUser(APP_API, c -> count(c, lookup, sha256("outro")));
    // sem usuário, a tabela continua fechada para app_api
    long direct =
        DbSession.asWithoutUser(
            APP_API,
            c -> count(c, "select count(*) from guardian_consent where user_id = ?", teen));

    assertThat(withHash).isEqualTo(1);
    assertThat(withOther).isZero();
    assertThat(direct).isZero();
  }

  @Test
  void decisaoValeUmaVezEApagaOSegredo() throws SQLException {
    var teen = user();
    var hash = sha256("segredo-" + teen);
    exec(
        "insert into guardian_consent(user_id, guardian_name, relationship, doc_version,"
            + " token_hash, token_expires_at) values (?, 'Maria', 'mother', 'v1', ?,"
            + " now() + interval '7 days')",
        teen,
        hash);
    var decide =
        "select guardian_request_decide(?, true, 'v2', now(), cast('203.0.113.9' as inet),"
            + " 'Navegador')";

    var first = DbSession.committed(APP_API, null, c -> single(c, decide, hash));
    var second = DbSession.committed(APP_API, null, c -> single(c, decide, hash));

    assertThat(first).isEqualTo(true);
    assertThat(second).isEqualTo(false);
    assertThat(
            single(
                "select (verified_at is not null) || ' ' || doc_version || ' ' ||"
                    + " (token_hash is null) from guardian_consent where user_id = ?",
                teen))
        .isEqualTo("true v2 true");
  }

  @Test
  void linkVencidoNaoDecide() throws SQLException {
    var teen = user();
    var hash = sha256("segredo-" + teen);
    exec(
        "insert into guardian_consent(user_id, guardian_name, relationship, doc_version,"
            + " granted_at, token_hash, token_expires_at) values (?, 'Maria', 'mother', 'v1',"
            + " now() - interval '9 days', ?, now() - interval '1 day')",
        teen,
        hash);

    var decided =
        DbSession.asWithoutUser(
            APP_API,
            c ->
                single(
                    c, "select guardian_request_decide(?, true, 'v1', now(), null, null)", hash));

    assertThat(decided).isEqualTo(false);
  }

  private static byte[] sha256(String value) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
