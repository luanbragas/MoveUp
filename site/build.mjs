// Monta site/dist: copia static/ e gera os arquivos de universal links / app links.
//   node build.mjs            produção: falha se faltar MOVEUP_APPLE_TEAM_ID ou MOVEUP_ANDROID_SHA256
//   node build.mjs --preview  prévia: gera só os arquivos que tiverem valor
import { cp, mkdir, rm, writeFile } from "node:fs/promises";
import { dirname, join } from "node:path";
import process from "node:process";
import { fileURLToPath } from "node:url";
import { appleAppSiteAssociation, assetLinks, parseFingerprints } from "./app-links.mjs";

const root = dirname(fileURLToPath(import.meta.url));
const dist = join(root, "dist");
const preview = process.argv.includes("--preview");

const teamId = process.env.MOVEUP_APPLE_TEAM_ID?.trim() ?? "";
const sha256 = process.env.MOVEUP_ANDROID_SHA256?.trim() ?? "";

const missing = [
  teamId === "" ? "MOVEUP_APPLE_TEAM_ID" : null,
  sha256 === "" ? "MOVEUP_ANDROID_SHA256" : null,
].filter((name) => name !== null);

if (missing.length > 0 && !preview) {
  process.stderr.write(
    `Faltam ${missing.join(" e ")}: sem eles o link do convite não abre o app.\n` +
      "Use --preview para montar o site sem esses arquivos (veja site/README.md).\n",
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

const skipped = missing.length === 0 ? "" : ` (prévia, sem: ${missing.join(", ")})`;
process.stdout.write(`site/dist pronto${skipped}\n`);
