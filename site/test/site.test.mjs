import assert from "node:assert/strict";
import { execFile } from "node:child_process";
import { readFile, stat } from "node:fs/promises";
import { dirname, join } from "node:path";
import { describe, it } from "node:test";
import { fileURLToPath } from "node:url";
import process from "node:process";
import { promisify } from "node:util";
import { appleAppSiteAssociation, assetLinks, parseFingerprints } from "../app-links.mjs";
import { appLink, inviteCodeFromPath } from "../static/assets/code.js";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const run = promisify(execFile);
const FINGERPRINT = Array.from({ length: 32 }, () => "AB").join(":");

describe("código do convite na URL", () => {
  it("aceita o código com ou sem barra final e normaliza", () => {
    assert.equal(inviteCodeFromPath("/i/K7M2QX9P"), "K7M2QX9P");
    assert.equal(inviteCodeFromPath("/i/k7m2qx9p/"), "K7M2QX9P");
    assert.equal(inviteCodeFromPath("/i/K7M2%20QX9P"), "K7M2QX9P");
    assert.equal(appLink("K7M2QX9P"), "moveup://i/K7M2QX9P");
  });

  it("recusa caminho, tamanho, alfabeto e escape inválidos", () => {
    for (const path of ["/", "/i/", "/i/K7M2QX9", "/i/K7M2QX9O", "/i/K7M2QX9P/x", "/i/%E0%A4%A"]) {
      assert.equal(inviteCodeFromPath(path), null, path);
    }
  });
});

describe("universal links e app links", () => {
  it("apple-app-site-association só abre o app no caminho do convite", () => {
    const aasa = appleAppSiteAssociation("ABCDE12345");
    assert.deepEqual(aasa.applinks.details[0].appIDs, ["ABCDE12345.br.com.moveup"]);
    assert.equal(aasa.applinks.details[0].components[0]["/"], "/i/*");
    assert.throws(() => appleAppSiteAssociation("abc"), /MOVEUP_APPLE_TEAM_ID/);
  });

  it("assetlinks aceita vários certificados e recusa formato errado", () => {
    const fingerprints = parseFingerprints(` ${FINGERPRINT.toLowerCase()} , ${FINGERPRINT} `);
    assert.equal(fingerprints.length, 2);
    assert.equal(fingerprints[0], FINGERPRINT);
    assert.equal(assetLinks(fingerprints)[0].target.package_name, "br.com.moveup");
    assert.throws(() => parseFingerprints("AB:CD"), /MOVEUP_ANDROID_SHA256/);
    assert.throws(() => parseFingerprints(" , "), /MOVEUP_ANDROID_SHA256/);
  });
});

describe("build", () => {
  const env = (extra) => {
    const base = { ...process.env };
    delete base.MOVEUP_APPLE_TEAM_ID;
    delete base.MOVEUP_ANDROID_SHA256;
    return { ...base, ...extra };
  };

  it("produção falha sem o certificado Android", async () => {
    await assert.rejects(
      run("node", ["build.mjs"], { cwd: root, env: env({ MOVEUP_APPLE_TEAM_ID: "ABCDE12345" }) }),
      /MOVEUP_ANDROID_SHA256/,
    );
  });

  it("sem conta Apple, gera só o arquivo do Android", async () => {
    await run("node", ["build.mjs"], {
      cwd: root,
      env: env({ MOVEUP_ANDROID_SHA256: FINGERPRINT }),
    });
    assert.ok((await stat(join(root, "dist/.well-known/assetlinks.json"))).isFile());
    await assert.rejects(stat(join(root, "dist/.well-known/apple-app-site-association")));
  });

  it("gera os dois arquivos com os identificadores", async () => {
    await run("node", ["build.mjs"], {
      cwd: root,
      env: env({ MOVEUP_APPLE_TEAM_ID: "ABCDE12345", MOVEUP_ANDROID_SHA256: FINGERPRINT }),
    });
    const aasa = JSON.parse(
      await readFile(join(root, "dist/.well-known/apple-app-site-association"), "utf8"),
    );
    const links = JSON.parse(
      await readFile(join(root, "dist/.well-known/assetlinks.json"), "utf8"),
    );
    assert.deepEqual(aasa.applinks.details[0].appIDs, ["ABCDE12345.br.com.moveup"]);
    assert.deepEqual(links[0].target.sha256_cert_fingerprints, [FINGERPRINT]);
    assert.ok((await stat(join(root, "dist/i/index.html"))).isFile());
  });
});
