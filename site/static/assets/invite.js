import { appLink, inviteCodeFromPath } from "./code.js";

// Só textContent e atributos: o código vem da URL e nunca vira HTML.
const code = inviteCodeFromPath(window.location.pathname);
const valid = document.getElementById("valid");
const invalid = document.getElementById("invalid");

if (code === null) {
  invalid.hidden = false;
} else {
  document.getElementById("code").textContent = code;
  document.getElementById("open").setAttribute("href", appLink(code));
  valid.hidden = false;
}
