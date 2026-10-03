package br.com.moveup.shared.infrastructure.crypto;

import com.google.crypto.tink.Aead;
import com.google.crypto.tink.InsecureSecretKeyAccess;
import com.google.crypto.tink.RegistryConfiguration;
import com.google.crypto.tink.TinkJsonProtoKeysetFormat;
import com.google.crypto.tink.aead.AeadConfig;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

/**
 * Chave mestra que cifra as DEKs dos alunos. Em produção é a chave do AWS KMS (adaptador na infra
 * de produção, Fase 9); local e testes usam um keyset Tink vindo do ambiente ({@code
 * MOVEUP_LOCAL_KEK}), nunca versionado.
 *
 * @param id vai para {@code client_key.kms_key_id} (rotação: saber com qual chave foi cifrada)
 */
public record KeyEncryptionKey(String id, Aead aead) {

  /** {@code base64(json do keyset Tink)}; sem valor o app não sobe (falha fechada). */
  public static KeyEncryptionKey local(String base64Keyset) {
    if (base64Keyset == null || base64Keyset.isBlank()) {
      throw new IllegalStateException(
          "moveup.crypto.local-kek ausente: gere com scripts/generate-local-kek e ponha no .env");
    }
    try {
      AeadConfig.register();
      var json =
          new String(Base64.getDecoder().decode(base64Keyset.strip()), StandardCharsets.UTF_8);
      var handle = TinkJsonProtoKeysetFormat.parseKeyset(json, InsecureSecretKeyAccess.get());
      return new KeyEncryptionKey(
          "local:" + handle.getKeysetInfo().getPrimaryKeyId(),
          handle.getPrimitive(RegistryConfiguration.get(), Aead.class));
    } catch (GeneralSecurityException | IllegalArgumentException e) {
      throw new IllegalStateException("moveup.crypto.local-kek inválida", e);
    }
  }
}
