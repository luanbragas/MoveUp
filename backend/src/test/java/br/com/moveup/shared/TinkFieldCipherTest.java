package br.com.moveup.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.moveup.db.PostgresTestDatabase;
import br.com.moveup.shared.infrastructure.crypto.KeyEncryptionKey;
import br.com.moveup.shared.infrastructure.crypto.TinkFieldCipher;
import br.com.moveup.shared.infrastructure.persistence.JooqClientKeys;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Criptografia de campo sem banco: a DEK fica num mapa no lugar da tabela client_key. */
class TinkFieldCipherTest {

  private final Map<UUID, JooqClientKeys.StoredKey> table = new HashMap<>();
  private final TinkFieldCipher cipher = cipher(PostgresTestDatabase.testKek());

  private TinkFieldCipher cipher(String kek) {
    var keys = mock(JooqClientKeys.class);
    when(keys.find(any())).thenAnswer(i -> Optional.ofNullable(table.get(i.<UUID>getArgument(0))));
    doAnswer(
            i -> {
              table.putIfAbsent(
                  i.getArgument(0),
                  new JooqClientKeys.StoredKey(i.getArgument(1), i.getArgument(2)));
              return null;
            })
        .when(keys)
        .insertIfAbsent(any(), any(), eq(KeyEncryptionKey.local(kek).id()));
    return new TinkFieldCipher(keys, KeyEncryptionKey.local(kek), Clock.systemUTC());
  }

  @Test
  void idaEVoltaComUmaChavePorAluno() {
    var bia = UUID.randomUUID();
    var stored = cipher.encrypt(bia, "anamnesis.answers", "Losartana 50 mg");

    assertThat(stored).startsWith("v1.").doesNotContain("Losartana");
    assertThat(cipher.decrypt(bia, "anamnesis.answers", stored)).isEqualTo("Losartana 50 mg");
    assertThat(table).containsOnlyKeys(bia);
    // mesmo texto, outra cifra (nonce aleatório)
    assertThat(cipher.encrypt(bia, "anamnesis.answers", "Losartana 50 mg")).isNotEqualTo(stored);
  }

  @Test
  void textoCifradoNaoServeEmOutroCampoNemParaOutroAluno() {
    var bia = UUID.randomUUID();
    var stored = cipher.encrypt(bia, "anamnesis.answers", "segredo");

    assertThatThrownBy(() -> cipher.decrypt(bia, "health_restriction.description", stored))
        .isInstanceOf(TinkFieldCipher.FieldCipherException.class)
        .hasMessage("field-cipher-decrypt");
    var caio = UUID.randomUUID();
    assertThatThrownBy(() -> cipher.decrypt(caio, "anamnesis.answers", stored))
        .isInstanceOf(TinkFieldCipher.FieldCipherException.class);
  }

  @Test
  void apagarAChaveDoAlunoTornaOCampoIlegivel() {
    var bia = UUID.randomUUID();
    var stored = cipher.encrypt(bia, "anamnesis.answers", "segredo");
    table.remove(bia);
    // outra instância (sem a DEK em cache), como depois de reiniciar a API
    var fresh = cipher(PostgresTestDatabase.testKek());
    assertThatThrownBy(() -> fresh.decrypt(bia, "anamnesis.answers", stored))
        .isInstanceOf(TinkFieldCipher.FieldCipherException.class);
  }

  @Test
  void valorGravadoAntesDaCriptografiaVoltaComoEsta() {
    assertThat(cipher.decrypt(UUID.randomUUID(), "session_feedback.comment", "texto antigo"))
        .isEqualTo("texto antigo");
  }
}
