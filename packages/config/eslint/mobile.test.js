// Prova que as regras de camada do app (mobile.js) pegam violações e deixam passar o
// permitido. Lint em memória sobre arquivos "virtuais" dentro de apps/mobile, que
// importam arquivos reais do app (a regra resolve os imports pelo disco).
import { URL, fileURLToPath } from "node:url";
import { ESLint } from "eslint";
import tseslint from "typescript-eslint";
import { describe, expect, it } from "vitest";
import { mobileConfig } from "./mobile.js";

const repoRoot = fileURLToPath(new URL("../../../", import.meta.url));
const appDir = `${repoRoot}apps/mobile`;

const eslint = new ESLint({
  cwd: repoRoot,
  overrideConfigFile: true,
  overrideConfig: [
    { files: ["apps/mobile/**/*.{ts,tsx}"], languageOptions: { parser: tseslint.parser } },
    ...mobileConfig({ appDir, files: ["apps/mobile/**/*.{ts,tsx,js}"] }),
  ],
});

async function boundaryErrors(file, code) {
  const [result] = await eslint.lintText(code, { filePath: `${appDir}/${file}` });
  return result.messages.filter((m) => m.ruleId === "boundaries/dependencies");
}

describe("regras de camada do app", () => {
  it("ui não importa data", async () => {
    const errors = await boundaryErrors(
      "src/features/auth/ui/Fixture.tsx",
      'import { createMeApiRepository } from "../data/api/me-api";\nexport const x = createMeApiRepository;\n',
    );
    expect(errors).toHaveLength(1);
  });

  it("ui e hooks não importam o api-client", async () => {
    for (const file of ["src/features/auth/ui/Fixture.tsx", "src/features/auth/hooks/fixture.ts"]) {
      const errors = await boundaryErrors(
        file,
        'import { getMe } from "@moveup/api-client";\nexport const x = getMe;\n',
      );
      expect(errors, file).toHaveLength(1);
    }
  });

  it("domain não importa React", async () => {
    const errors = await boundaryErrors(
      "src/features/auth/domain/fixture.ts",
      'import { useState } from "react";\nexport const x = useState;\n',
    );
    expect(errors).toHaveLength(1);
  });

  it("outra feature só entra pelo index.ts", async () => {
    const errors = await boundaryErrors(
      "src/features/training/ui/Fixture.tsx",
      [
        'import { useMe as internal } from "../../auth/hooks/use-me";',
        'import { useMe } from "../../auth";',
        "export const x = [internal, useMe];",
        "",
      ].join("\n"),
    );
    expect(errors).toHaveLength(1);
    expect(errors[0]?.line).toBe(1);
  });

  it("o permitido passa: hooks usam providers e domain; data usa api-client e shared", async () => {
    expect(
      await boundaryErrors(
        "src/features/auth/hooks/fixture.ts",
        [
          'import { useQuery } from "@tanstack/react-query";',
          'import { useRepositories } from "../../../providers/repositories";',
          'import type { Me } from "../domain/me";',
          "export const x: [typeof useQuery, typeof useRepositories, Me | null] = [useQuery, useRepositories, null];",
          "",
        ].join("\n"),
      ),
    ).toHaveLength(0);
    expect(
      await boundaryErrors(
        "src/features/auth/data/api/fixture.ts",
        [
          'import { getMe } from "@moveup/api-client";',
          'import { callApi } from "../../../../shared/lib/http";',
          "export const x = [getMe, callApi];",
          "",
        ].join("\n"),
      ),
    ).toHaveLength(0);
  });
});
