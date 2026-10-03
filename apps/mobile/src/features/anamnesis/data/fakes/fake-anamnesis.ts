import type { AnamnesisRecord, Restriction, Template } from "../../domain/anamnesis";
import type { AnamnesisRepository } from "../../domain/ports";

/** Modelo pequeno com as quatro seções (o real vem do servidor). */
export const FAKE_TEMPLATE: Template = {
  version: 1,
  questions: [
    {
      code: "goal",
      section: "goal",
      label: "Qual é o seu principal objetivo?",
      type: "single",
      required: true,
      options: [
        { value: "hypertrophy", label: "Ganhar massa muscular" },
        { value: "health", label: "Saúde e qualidade de vida" },
      ],
      min: null,
      max: null,
    },
    {
      code: "weekly_days",
      section: "routine",
      label: "Quantos dias por semana pode treinar?",
      type: "integer",
      required: true,
      options: [],
      min: 1,
      max: 7,
    },
    {
      code: "parq_heart",
      section: "parq",
      label: "Problema no coração?",
      type: "yes_no",
      required: true,
      options: [],
      min: null,
      max: null,
    },
    {
      code: "medications",
      section: "health",
      label: "Remédios que usa",
      type: "text",
      required: false,
      options: [],
      min: null,
      max: null,
    },
  ],
};

/** Anamnese e restrições em memória para testes (versão e liberação como no servidor). */
export function createFakeAnamnesis(
  restrictions: readonly Restriction[] = [],
): AnamnesisRepository & { readonly submitted: AnamnesisRecord[] } {
  let mine: AnamnesisRecord | null = null;
  let list = [...restrictions];
  const submitted: AnamnesisRecord[] = [];
  return {
    submitted,
    template: () => Promise.resolve(FAKE_TEMPLATE),
    mine: () => Promise.resolve(mine),
    submit(answers) {
      const parq = answers["parq_heart"] === true;
      mine = {
        versionNumber:
          mine === null ? 1 : mine.reviewed ? mine.versionNumber + 1 : mine.versionNumber,
        answers,
        parqPositive: parq,
        clearance: parq ? "pending" : "not_required",
        clearanceDate: null,
        reviewed: false,
        reviewedAt: null,
        createdAt: new Date(),
      };
      submitted.push(mine);
      return Promise.resolve(mine);
    },
    ofClient: () => Promise.resolve({ latest: mine, versions: [] }),
    review(_linkId, answers, clearance, clearanceDate) {
      mine = {
        versionNumber: mine?.versionNumber ?? 1,
        answers,
        parqPositive: answers["parq_heart"] === true,
        clearance,
        clearanceDate,
        reviewed: true,
        reviewedAt: new Date(),
        createdAt: new Date(),
      };
      return Promise.resolve(mine);
    },
    restrictions: () => Promise.resolve(list),
    createRestriction(_linkId, input) {
      const created = { ...input, id: `r${String(list.length + 1)}`, fromAnamnesis: false };
      list = [...list, created];
      return Promise.resolve(created);
    },
    updateRestriction(_linkId, id, input) {
      const updated = { ...input, id, fromAnamnesis: false };
      list = list.map((r) => (r.id === id ? updated : r));
      return Promise.resolve(updated);
    },
    deleteRestriction(_linkId, id) {
      list = list.filter((r) => r.id !== id);
      return Promise.resolve();
    },
  };
}
