// Gera a chave mestra LOCAL (keyset Tink AES-256-GCM em JSON, codificado em base64) para o .env:
//   node backend/scripts/generate-local-kek.mjs
// Só para desenvolvimento e testes: produção usa o AWS KMS. Nunca commite o valor.
import { randomBytes, randomInt } from "node:crypto";

const key = randomBytes(32);
// proto AesGcmKey { version = 0; key_value = 3 (bytes) }
const proto = Buffer.concat([Buffer.from([0x1a, key.length]), key]);
const keyId = randomInt(1, 2 ** 31 - 1);
const keyset = {
  primaryKeyId: keyId,
  key: [
    {
      keyData: {
        typeUrl: "type.googleapis.com/google.crypto.tink.AesGcmKey",
        value: proto.toString("base64"),
        keyMaterialType: "SYMMETRIC",
      },
      status: "ENABLED",
      keyId,
      outputPrefixType: "TINK",
    },
  ],
};
process.stdout.write(`MOVEUP_LOCAL_KEK=${Buffer.from(JSON.stringify(keyset)).toString("base64")}\n`);
