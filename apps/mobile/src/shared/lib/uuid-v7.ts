import { getRandomBytes } from "expo-crypto";

/**
 * UUIDv7 (RFC 9562): 48 bits de milissegundos, versão 7, variante RFC e o resto aleatório
 * (expo-crypto). Ordenável pelo tempo: bom para índice. Id dos registros offline (CLAUDE.md, 9).
 */
export function uuidV7(
  now: number = Date.now(),
  random: (n: number) => Uint8Array = getRandomBytes,
): string {
  const bytes = random(16);
  let ms = Math.max(0, Math.floor(now));
  for (let i = 5; i >= 0; i -= 1) {
    bytes[i] = ms % 256;
    ms = Math.floor(ms / 256);
  }
  bytes[6] = ((bytes[6] ?? 0) & 0x0f) | 0x70;
  bytes[8] = ((bytes[8] ?? 0) & 0x3f) | 0x80;
  const hex = Array.from(bytes, (b) => b.toString(16).padStart(2, "0")).join("");
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}
