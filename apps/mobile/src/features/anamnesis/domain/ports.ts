import type {
  AnamnesisRecord,
  Answers,
  Clearance,
  Restriction,
  RestrictionInput,
  Template,
  VersionSummary,
} from "./anamnesis";

/** Anamnese e restrições (backend: módulo anamnesis). */
export interface AnamnesisRepository {
  template(): Promise<Template>;
  mine(): Promise<AnamnesisRecord | null>;
  submit(answers: Answers): Promise<AnamnesisRecord>;
  ofClient(linkId: string): Promise<{
    readonly latest: AnamnesisRecord | null;
    readonly versions: readonly VersionSummary[];
  }>;
  review(
    linkId: string,
    answers: Answers,
    clearance: Clearance,
    clearanceDate: string | null,
  ): Promise<AnamnesisRecord>;
  restrictions(linkId: string, includeResolved: boolean): Promise<readonly Restriction[]>;
  createRestriction(linkId: string, input: RestrictionInput): Promise<Restriction>;
  updateRestriction(linkId: string, id: string, input: RestrictionInput): Promise<Restriction>;
  deleteRestriction(linkId: string, id: string): Promise<void>;
}

/** Rascunho da anamnese do aluno no aparelho (terminar depois). */
export interface AnamnesisDraftStore {
  get(): Promise<Answers | null>;
  save(answers: Answers): Promise<void>;
  clear(): Promise<void>;
}
