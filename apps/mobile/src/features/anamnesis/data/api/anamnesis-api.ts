import {
  createRestriction,
  deleteRestriction,
  getAnamnesisTemplate,
  getClientAnamnesis,
  getMyAnamnesis,
  listRestrictions,
  reviewClientAnamnesis,
  schemas,
  submitMyAnamnesis,
  updateRestriction,
} from "@moveup/api-client";
import { z } from "zod";
import { callApi } from "../../../../shared/lib/http";
import type {
  AnamnesisRecord,
  Answers,
  AnswerValue,
  Restriction,
  RestrictionInput,
} from "../../domain/anamnesis";
import type { AnamnesisRepository } from "../../domain/ports";

const NoContent = z.undefined();

type RecordDto = z.infer<typeof schemas.SubmitMyAnamnesis200Response>;
type RestrictionDto = z.infer<typeof schemas.CreateRestriction201Response>;

/** Resposta vinda do servidor: só os formatos que o modelo aceita (o resto é descartado). */
function toAnswers(raw: Record<string, unknown>): Answers {
  const out: Record<string, AnswerValue> = {};
  for (const [code, value] of Object.entries(raw)) {
    if (typeof value === "string" || typeof value === "number" || typeof value === "boolean") {
      out[code] = value;
    } else if (Array.isArray(value) && value.every((v) => typeof v === "string")) {
      out[code] = value;
    }
  }
  return out;
}

function toRecord(dto: RecordDto): AnamnesisRecord {
  return {
    versionNumber: dto.versionNumber,
    answers: toAnswers(dto.answers),
    parqPositive: dto.parqPositive,
    clearance: dto.clearance,
    clearanceDate: dto.clearanceDate ?? null,
    reviewed: dto.reviewed,
    reviewedAt: dto.reviewedAt == null ? null : new Date(dto.reviewedAt),
    createdAt: new Date(dto.createdAt),
  };
}

function toRestriction(dto: RestrictionDto): Restriction {
  return {
    id: dto.id,
    kind: dto.kind,
    bodyRegion: dto.bodyRegion ?? null,
    description: dto.description,
    severity: dto.severity ?? null,
    resolvedOn: dto.resolvedOn ?? null,
    fromAnamnesis: dto.fromAnamnesis,
  };
}

function restrictionBody(input: RestrictionInput) {
  return {
    kind: input.kind,
    description: input.description,
    ...(input.bodyRegion === null ? {} : { bodyRegion: input.bodyRegion }),
    ...(input.severity === null ? {} : { severity: input.severity }),
    ...(input.resolvedOn === null ? {} : { resolvedOn: input.resolvedOn }),
  };
}

export function createAnamnesisApiRepository(): AnamnesisRepository {
  return {
    async template() {
      const dto = await callApi(
        () => getAnamnesisTemplate(),
        schemas.GetAnamnesisTemplate200Response,
      );
      return {
        version: dto.version,
        questions: dto.questions.map((q) => ({
          code: q.code,
          section: q.section,
          label: q.label,
          type: q.type,
          required: q.required,
          options: q.options,
          min: q.min ?? null,
          max: q.max ?? null,
        })),
      };
    },
    async mine() {
      const dto = await callApi(() => getMyAnamnesis(), schemas.GetMyAnamnesis200Response);
      return dto.anamnesis === undefined ? null : toRecord(dto.anamnesis);
    },
    async submit(answers) {
      const dto = await callApi(
        () => submitMyAnamnesis({ answers: { ...answers } }),
        schemas.SubmitMyAnamnesis200Response,
      );
      return toRecord(dto);
    },
    async ofClient(linkId) {
      const dto = await callApi(
        () => getClientAnamnesis(linkId),
        schemas.GetClientAnamnesis200Response,
      );
      return {
        latest: dto.latest === undefined ? null : toRecord(dto.latest),
        versions: dto.versions.map((v) => ({
          versionNumber: v.versionNumber,
          createdAt: new Date(v.createdAt),
          reviewedAt: v.reviewedAt == null ? null : new Date(v.reviewedAt),
          clearance: v.clearance,
          parqPositive: v.parqPositive,
        })),
      };
    },
    async review(linkId, answers, clearance, clearanceDate) {
      const dto = await callApi(
        () =>
          reviewClientAnamnesis(linkId, {
            answers: { ...answers },
            clearance,
            ...(clearanceDate === null ? {} : { clearanceDate }),
          }),
        schemas.ReviewClientAnamnesis200Response,
      );
      return toRecord(dto);
    },
    async restrictions(linkId, includeResolved) {
      const list = await callApi(
        () => listRestrictions(linkId, { includeResolved }),
        schemas.ListRestrictions200Response,
      );
      return list.map(toRestriction);
    },
    async createRestriction(linkId, input) {
      return toRestriction(
        await callApi(
          () => createRestriction(linkId, restrictionBody(input)),
          schemas.CreateRestriction201Response,
        ),
      );
    },
    async updateRestriction(linkId, id, input) {
      return toRestriction(
        await callApi(
          () => updateRestriction(linkId, id, restrictionBody(input)),
          schemas.UpdateRestriction200Response,
        ),
      );
    },
    async deleteRestriction(linkId, id) {
      await callApi(() => deleteRestriction(linkId, id), NoContent);
    },
  };
}
