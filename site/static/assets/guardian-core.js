// Regras da página do responsável, sem DOM (testadas em test/site.test.mjs).

/* global URL */
const TOKEN = /^[A-Za-z0-9_-]{43}$/;

const RELATIONSHIPS = {
  mother: "mãe",
  father: "pai",
  legal_guardian: "responsável legal",
  other: "responsável",
};

/** Segredo do link: vem depois do # (o navegador não manda essa parte ao servidor do site). */
export function tokenFromHash(hash) {
  const token = typeof hash === "string" ? hash.replace(/^#/, "") : "";
  return TOKEN.test(token) ? token : null;
}

export function relationshipLabel(code) {
  return RELATIONSHIPS[code] ?? RELATIONSHIPS.other;
}

/** Endereço da API sem barra no fim; vazio ou inválido = página indisponível. */
export function apiBase(value) {
  if (typeof value !== "string" || value.trim() === "") {
    return null;
  }
  try {
    const url = new URL(value.trim());
    return url.protocol === "https:" || url.hostname === "localhost"
      ? url.href.replace(/\/+$/, "")
      : null;
  } catch {
    return null;
  }
}

/**
 * Chamadas da página. Resultado sempre num destes formatos:
 *   { kind: "ok", data } · { kind: "gone" } (link inválido, vencido ou usado)
 *   { kind: "outdated" } (o termo mudou) · { kind: "failed" } (rede ou erro inesperado)
 */
export function guardianApi(base, fetchImpl) {
  async function post(path, body) {
    let response;
    try {
      response = await fetchImpl(`${base}${path}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
        credentials: "omit",
        referrerPolicy: "no-referrer",
      });
    } catch {
      return { kind: "failed" };
    }
    if (response.status === 404) {
      return { kind: "gone" };
    }
    if (response.status === 409) {
      return { kind: "outdated" };
    }
    if (!response.ok) {
      return { kind: "failed" };
    }
    if (response.status === 204) {
      return { kind: "ok", data: null };
    }
    try {
      return { kind: "ok", data: await response.json() };
    } catch {
      return { kind: "failed" };
    }
  }

  return {
    preview: (token) => post("/v1/guardian-authorizations/preview", { token }),
    decide: (token, approve, docVersion) =>
      post("/v1/guardian-authorizations/decision", { token, approve, docVersion }),
  };
}
