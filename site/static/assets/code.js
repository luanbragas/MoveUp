// Código do convite a partir do endereço (mesmas regras do app: 8 caracteres, sem O, I, L, 0, 1).
const CODE = /^[ABCDEFGHJKMNPQRSTUVWXYZ23456789]{8}$/;
const PATH = /^\/i\/([^/]+)\/?$/;

/** `/i/k7m2qx9p` → "K7M2QX9P"; qualquer outra coisa → null. */
export function inviteCodeFromPath(pathname) {
  const match = PATH.exec(pathname);
  if (match === null) {
    return null;
  }
  let raw;
  try {
    raw = decodeURIComponent(match[1]);
  } catch {
    return null;
  }
  const code = raw.replace(/\s+/g, "").toUpperCase();
  return CODE.test(code) ? code : null;
}

/** Abre o app instalado pelo esquema próprio (plano B quando o universal link não abriu). */
export function appLink(code) {
  return `moveup://i/${code}`;
}
