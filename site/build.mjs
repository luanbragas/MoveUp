// Monta site/dist: copia static/, gera os arquivos de universal links / app links e aponta a
// página do responsável para a API.
//   node build.mjs            produção: falha sem MOVEUP_ANDROID_SHA256; o da Apple é opcional
//                             (sem conta Apple Developer não há universal link no iPhone)
//   node build.mjs --preview  prévia: gera só os arquivos que tiverem valor
// MOVEUP_API_BASE_URL (opcional): endereço da API; sem ele a página do responsável diz "em breve".
import { cp, mkdir, readFile, rm, writeFile } from "node:fs/promises";
import { dirname, join } from "node:path";
import process from "node:process";
import { URL, fileURLToPath } from "node:url";
import { appleAppSiteAssociation, assetLinks, parseFingerprints } from "./app-links.mjs";
import { apiBase } from "./static/assets/guardian-core.js";

const root = dirname(fileURLToPath(import.meta.url));
const dist = join(root, "dist");
const preview = process.argv.includes("--preview");

const teamId = process.env.MOVEUP_APPLE_TEAM_ID?.trim() ?? "";
const sha256 = process.env.MOVEUP_ANDROID_SHA256?.trim() ?? "";
const rawApi = process.env.MOVEUP_API_BASE_URL?.trim() ?? "";
const api = apiBase(rawApi);

if (rawApi !== "" && api === null) {
  process.stderr.write("MOVEUP_API_BASE_URL inválida: use https:// (ou http://localhost).\n");
  process.exit(1);
}

const missing = [
  teamId === "" ? "MOVEUP_APPLE_TEAM_ID" : null,
  sha256 === "" ? "MOVEUP_ANDROID_SHA256" : null,
  api === null ? "MOVEUP_API_BASE_URL" : null,
].filter((name) => name !== null);

if (sha256 === "" && !preview) {
  process.stderr.write(
    "Falta MOVEUP_ANDROID_SHA256: sem ele o link do convite não abre o app no Android.\n" +
      "Use --preview para montar o site sem esse arquivo (veja site/README.md).\n",
  );
  process.exit(1);
}

await rm(dist, { recursive: true, force: true });
await cp(join(root, "static"), dist, { recursive: true });
await mkdir(join(dist, ".well-known"), { recursive: true });

const json = (value) => `${JSON.stringify(value, null, 2)}\n`;
if (teamId !== "") {
  await writeFile(
    join(dist, ".well-known", "apple-app-site-association"),
    json(appleAppSiteAssociation(teamId)),
  );
}
if (sha256 !== "") {
  await writeFile(
    join(dist, ".well-known", "assetlinks.json"),
    json(assetLinks(parseFingerprints(sha256))),
  );
}

if (api !== null) {
  // A página do responsável chama a API: só essa origem entra no connect-src do CSP.
  await writeFile(
    join(dist, "assets", "config.js"),
    `export const API_BASE_URL = ${JSON.stringify(api)};
`,
  );
  const headersPath = join(dist, "_headers");
  const headers = await readFile(headersPath, "utf8");
  await writeFile(
    headersPath,
    headers.replace(
      "default-src 'none';",
      `default-src 'none'; connect-src ${new URL(api).origin};`,
    ),
  );
}

const skipped = missing.length === 0 ? "" : ` (sem: ${missing.join(", ")})`;
process.stdout.write(`site/dist pronto${skipped}\n`);
