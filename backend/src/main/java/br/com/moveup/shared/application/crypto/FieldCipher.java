package br.com.moveup.shared.application.crypto;

import java.util.UUID;

/**
 * Criptografia de campo com dado de saúde (ARQUITETURA 7.3): AES-256-GCM com a chave de dados do
 * aluno. O domínio trabalha com o texto em claro; cifra e decifra acontecem no adaptador de
 * persistência. Apagar a chave do aluno torna o campo ilegível em todo lugar (crypto-shredding).
 */
public interface FieldCipher {

  /**
   * @param field onde o valor mora (ex.: {@code anamnesis.answers}): vai como dado associado, então
   *     o texto cifrado de um campo não serve em outro nem para outro aluno
   */
  String encrypt(UUID clientId, String field, String plaintext);

  /** Valor sem o prefixo de cifrado (gravado antes da criptografia) volta como está. */
  String decrypt(UUID clientId, String field, String stored);

  /** Para gravar campo opcional sem tratar nulo em todo lugar. */
  default String encryptNullable(UUID clientId, String field, String plaintext) {
    return plaintext == null ? null : encrypt(clientId, field, plaintext);
  }

  default String decryptNullable(UUID clientId, String field, String stored) {
    return stored == null ? null : decrypt(clientId, field, stored);
  }
}
