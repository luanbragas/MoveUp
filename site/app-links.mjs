// Arquivos de universal links (iOS) e app links (Android) do convite.
// Gerados no build a partir de variáveis de ambiente: nunca publicamos valor de exemplo.

export const IOS_BUNDLE_ID = "br.com.moveup";
export const ANDROID_PACKAGE = "br.com.moveup";
/** Só o link do convite abre o app; o resto do site (termos, privacidade) fica no navegador. */
export const INVITE_PATH = "/i/*";

const TEAM_ID = /^[A-Z0-9]{10}$/;
const SHA256 = /^([0-9A-F]{2}:){31}[0-9A-F]{2}$/;

/** `.well-known/apple-app-site-association` (sem extensão, servido como JSON). */
export function appleAppSiteAssociation(teamId) {
  if (!TEAM_ID.test(teamId)) {
    throw new Error("MOVEUP_APPLE_TEAM_ID inválido: são 10 letras maiúsculas e números");
  }
  return {
    applinks: {
      details: [
        {
          appIDs: [`${teamId}.${IOS_BUNDLE_ID}`],
          components: [{ "/": INVITE_PATH, comment: "Link do convite" }],
        },
      ],
    },
  };
}

/** Lista separada por vírgula (certificado de upload e o da Play App Signing, por exemplo). */
export function parseFingerprints(raw) {
  const fingerprints = raw
    .split(",")
    .map((value) => value.trim().toUpperCase())
    .filter((value) => value !== "");
  if (fingerprints.length === 0 || !fingerprints.every((value) => SHA256.test(value))) {
    throw new Error("MOVEUP_ANDROID_SHA256 inválido: use o formato AA:BB:... (32 pares)");
  }
  return fingerprints;
}

/** `.well-known/assetlinks.json`. */
export function assetLinks(fingerprints) {
  return [
    {
      relation: ["delegate_permission/common.handle_all_urls"],
      target: {
        namespace: "android_app",
        package_name: ANDROID_PACKAGE,
        sha256_cert_fingerprints: fingerprints,
      },
    },
  ];
}
