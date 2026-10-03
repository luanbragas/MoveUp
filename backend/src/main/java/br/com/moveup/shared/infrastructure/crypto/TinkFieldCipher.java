package br.com.moveup.shared.infrastructure.crypto;

import br.com.moveup.shared.application.crypto.FieldCipher;
import br.com.moveup.shared.infrastructure.persistence.JooqClientKeys;
import com.google.crypto.tink.Aead;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.RegistryConfiguration;
import com.google.crypto.tink.TinkProtoKeysetFormat;
import com.google.crypto.tink.aead.PredefinedAeadParameters;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * {@link FieldCipher} com Tink. Cada aluno tem uma DEK (keyset AES-256-GCM) guardada cifrada pela
 * chave mestra em {@code client_key}; a DEK decifrada fica alguns minutos em memória para não abrir
 * a chave mestra a cada campo. Formato gravado: {@code v1.<base64>}.
 */
public class TinkFieldCipher implements FieldCipher {

  static final String PREFIX = "v1.";
  private static final Duration CACHE_TTL = Duration.ofMinutes(5);
  private static final int CACHE_MAX = 10_000;

  private final JooqClientKeys keys;
  private final KeyEncryptionKey kek;
  private final Clock clock;
  private final Map<UUID, Cached> cache =
      new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, Cached> eldest) {
          return size() > CACHE_MAX;
        }
      };

  private record Cached(Aead aead, Instant until) {}

  public TinkFieldCipher(JooqClientKeys keys, KeyEncryptionKey kek, Clock clock) {
    this.keys = keys;
    this.kek = kek;
    this.clock = clock;
  }

  @Override
  public String encrypt(UUID clientId, String field, String plaintext) {
    try {
      var bytes =
          dek(clientId, true)
              .encrypt(plaintext.getBytes(StandardCharsets.UTF_8), associated(clientId, field));
      return PREFIX + Base64.getEncoder().encodeToString(bytes);
    } catch (GeneralSecurityException e) {
      throw new FieldCipherException("encrypt", e);
    }
  }

  @Override
  public String decrypt(UUID clientId, String field, String stored) {
    if (!stored.startsWith(PREFIX)) {
      return stored; // gravado antes da criptografia de campo
    }
    try {
      var bytes = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
      return new String(
          dek(clientId, false).decrypt(bytes, associated(clientId, field)), StandardCharsets.UTF_8);
    } catch (GeneralSecurityException | IllegalArgumentException e) {
      throw new FieldCipherException("decrypt", e);
    }
  }

  private static byte[] associated(UUID clientId, String field) {
    return ("moveup:" + field + ":" + clientId).getBytes(StandardCharsets.UTF_8);
  }

  /**
   * @param create cifrar cria a DEK do aluno na primeira vez; decifrar sem DEK falha (apagada no
   *     crypto-shredding)
   */
  private Aead dek(UUID clientId, boolean create) throws GeneralSecurityException {
    var now = clock.instant();
    synchronized (cache) {
      var hit = cache.get(clientId);
      if (hit != null && hit.until().isAfter(now)) {
        return hit.aead();
      }
    }
    var aead = load(clientId, create);
    synchronized (cache) {
      cache.put(clientId, new Cached(aead, now.plus(CACHE_TTL)));
    }
    return aead;
  }

  private Aead load(UUID clientId, boolean create) throws GeneralSecurityException {
    var stored = keys.find(clientId);
    if (stored.isEmpty() && create) {
      var fresh = KeysetHandle.generateNew(PredefinedAeadParameters.AES256_GCM);
      var wrapped =
          TinkProtoKeysetFormat.serializeEncryptedKeyset(
              fresh, kek.aead(), dekAssociated(clientId));
      // corrida entre duas gravações do mesmo aluno: vale a que entrou primeiro
      keys.insertIfAbsent(clientId, wrapped, kek.id());
      stored = keys.find(clientId);
    }
    var row = stored.orElseThrow(() -> new GeneralSecurityException("client_key ausente"));
    var handle =
        TinkProtoKeysetFormat.parseEncryptedKeyset(
            row.encryptedDek(), kek.aead(), dekAssociated(clientId));
    return handle.getPrimitive(RegistryConfiguration.get(), Aead.class);
  }

  private static byte[] dekAssociated(UUID clientId) {
    return ("moveup:client_key:" + clientId).getBytes(StandardCharsets.UTF_8);
  }

  /** Falha de criptografia: nunca leva o valor do campo na mensagem. */
  public static class FieldCipherException extends RuntimeException {
    FieldCipherException(String operation, Exception cause) {
      super("field-cipher-" + operation, cause);
    }
  }
}
