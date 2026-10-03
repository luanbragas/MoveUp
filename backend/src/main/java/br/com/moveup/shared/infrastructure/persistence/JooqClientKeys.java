package br.com.moveup.shared.infrastructure.persistence;

import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.CLIENT_KEY;

import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

/**
 * DEK cifrada de cada aluno ({@code client_key}). app_api lê e cria só a dos alunos que pode ler e
 * escrever (RLS); nunca altera nem apaga.
 */
@Repository
public class JooqClientKeys {

  private final DSLContext dsl;

  public JooqClientKeys(DSLContext dsl) {
    this.dsl = dsl;
  }

  public record StoredKey(byte[] encryptedDek, String kekId) {}

  public Optional<StoredKey> find(UUID clientId) {
    return dsl.select(CLIENT_KEY.ENCRYPTED_DEK, CLIENT_KEY.KMS_KEY_ID)
        .from(CLIENT_KEY)
        .where(CLIENT_KEY.CLIENT_ID.eq(clientId))
        .fetchOptional(r -> new StoredKey(r.value1(), r.value2()));
  }

  public void insertIfAbsent(UUID clientId, byte[] encryptedDek, String kekId) {
    dsl.insertInto(CLIENT_KEY)
        .set(CLIENT_KEY.CLIENT_ID, clientId)
        .set(CLIENT_KEY.ENCRYPTED_DEK, encryptedDek)
        .set(CLIENT_KEY.KMS_KEY_ID, kekId)
        .onConflict(CLIENT_KEY.CLIENT_ID)
        .doNothing()
        .execute();
  }
}
