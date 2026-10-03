import { API_BASE_URL } from "./config.js";
import { apiBase, guardianApi, relationshipLabel, tokenFromHash } from "./guardian-core.js";

// Só textContent e atributos: nada que vem da URL ou da API vira HTML.

const token = tokenFromHash(window.location.hash);
// Tira o segredo da barra de endereço (histórico, print de tela). Fica só em memória:
// "tentar de novo" refaz a consulta sem recarregar a página.
window.history.replaceState(null, "", window.location.pathname);

const base = apiBase(API_BASE_URL);
const api = base === null ? null : guardianApi(base, window.fetch.bind(window));
const sections = ["loading", "ask", "approved", "declined", "gone", "failed", "unavailable"];

const agree = document.getElementById("agree");
const approve = document.getElementById("approve");
const decline = document.getElementById("decline");
const notice = document.getElementById("ask-error");
let docVersion = null;

function show(id) {
  for (const section of sections) {
    document.getElementById(section).hidden = section !== id;
  }
  document.getElementById(id).querySelector("h1")?.focus();
}

function fill(className, text) {
  for (const element of document.querySelectorAll(`.${className}`)) {
    element.textContent = text;
  }
}

function say(text) {
  notice.textContent = text;
  notice.hidden = text === "";
}

async function load(message = "") {
  if (token === null) {
    show("gone");
    return;
  }
  if (api === null) {
    show("unavailable");
    return;
  }
  show("loading");
  const preview = await api.preview(token);
  if (preview.kind !== "ok") {
    show(preview.kind === "gone" ? "gone" : "failed");
    return;
  }
  const { minorFirstName, guardianName, relationship } = preview.data;
  docVersion = preview.data.docVersion;
  fill("minor", minorFirstName);
  fill("guardian", guardianName);
  fill("relationship", relationshipLabel(relationship));
  agree.checked = false;
  approve.disabled = true;
  decline.disabled = false;
  say(message);
  show("ask");
}

async function decide(approved) {
  approve.disabled = true;
  decline.disabled = true;
  say("");
  const result = await api.decide(token, approved, docVersion);
  if (result.kind === "ok") {
    show(approved ? "approved" : "declined");
  } else if (result.kind === "gone") {
    show("gone");
  } else if (result.kind === "outdated") {
    await load("O termo foi atualizado. Leia de novo e confirme.");
  } else {
    say("Não deu para enviar agora. Confira a internet e tente de novo.");
    approve.disabled = !agree.checked;
    decline.disabled = false;
  }
}

agree.addEventListener("change", () => {
  approve.disabled = !agree.checked;
});
approve.addEventListener("click", () => void decide(true));
decline.addEventListener("click", () => void decide(false));
document.getElementById("retry").addEventListener("click", () => void load());

void load();
