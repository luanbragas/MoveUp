// Anamnese e restrições no formato do app (não o DTO da API). Regras puras, sem React.

export type QuestionType = "single" | "multi" | "yes_no" | "integer" | "text";
export type Section = "goal" | "routine" | "parq" | "health";
export const SECTIONS: readonly Section[] = ["goal", "routine", "parq", "health"];

export interface Question {
  readonly code: string;
  readonly section: Section;
  readonly label: string;
  readonly type: QuestionType;
  readonly required: boolean;
  readonly options: readonly { readonly value: string; readonly label: string }[];
  readonly min: number | null;
  readonly max: number | null;
}

export interface Template {
  readonly version: number;
  readonly questions: readonly Question[];
}

export type AnswerValue = string | number | boolean | readonly string[];
export type Answers = Readonly<Record<string, AnswerValue>>;
export type Clearance = "not_required" | "pending" | "cleared";

export interface AnamnesisRecord {
  readonly versionNumber: number;
  readonly answers: Answers;
  readonly parqPositive: boolean;
  readonly clearance: Clearance;
  readonly clearanceDate: string | null;
  readonly reviewed: boolean;
  readonly reviewedAt: Date | null;
  readonly createdAt: Date;
}

export interface VersionSummary {
  readonly versionNumber: number;
  readonly createdAt: Date;
  readonly reviewedAt: Date | null;
  readonly clearance: Clearance;
  readonly parqPositive: boolean;
}

export type RestrictionKind = "injury" | "surgery" | "pain" | "condition";

export interface Restriction {
  readonly id: string;
  readonly kind: RestrictionKind;
  readonly bodyRegion: string | null;
  readonly description: string;
  readonly severity: number | null;
  readonly resolvedOn: string | null;
  readonly fromAnamnesis: boolean;
}

export type RestrictionInput = Omit<Restriction, "id" | "fromAnamnesis">;

export function questionsOf(template: Template, section: Section): readonly Question[] {
  return template.questions.filter((q) => q.section === section);
}

function answered(value: AnswerValue | undefined): boolean {
  if (value === undefined) {
    return false;
  }
  if (typeof value === "string") {
    return value.trim() !== "";
  }
  return Array.isArray(value) ? value.length > 0 : true;
}

/** Obrigatórias sem resposta (na seção, ou em todas). */
export function missing(
  template: Template,
  answers: Answers,
  section?: Section,
): readonly Question[] {
  return template.questions.filter(
    (q) =>
      q.required && (section === undefined || q.section === section) && !answered(answers[q.code]),
  );
}

/** Qualquer "sim" no PAR-Q: liberação médica recomendada (o servidor decide igual). */
export function parqPositive(template: Template, answers: Answers): boolean {
  return questionsOf(template, "parq").some((q) => answers[q.code] === true);
}

/** Muda (ou apaga, com undefined) a resposta de uma pergunta. */
export function withAnswer(
  answers: Answers,
  code: string,
  value: AnswerValue | undefined,
): Answers {
  const rest = Object.fromEntries(Object.entries(answers).filter(([key]) => key !== code));
  return value === undefined ? rest : { ...rest, [code]: value };
}

/** Resposta pronta para enviar: tira os vazios. */
export function cleaned(answers: Answers): Answers {
  return Object.fromEntries(Object.entries(answers).filter(([, value]) => answered(value)));
}

export function toggleOption(current: AnswerValue | undefined, value: string): readonly string[] {
  // AnswerValue só é objeto quando é lista
  const list: readonly string[] = typeof current === "object" ? current : [];
  return list.includes(value) ? list.filter((v) => v !== value) : [...list, value];
}

/** Grupos musculares (os mesmos do mapa e da biblioteca) que cada região do corpo envolve. */
const REGION_MUSCLES: Readonly<Record<string, readonly string[]>> = {
  neck: ["traps"],
  shoulder_left: ["delts", "chest", "traps"],
  shoulder_right: ["delts", "chest", "traps"],
  elbow_left: ["biceps", "triceps", "forearms"],
  elbow_right: ["biceps", "triceps", "forearms"],
  wrist_left: ["forearms"],
  wrist_right: ["forearms"],
  chest: ["chest"],
  upper_back: ["lats", "traps"],
  lower_back: ["lowerback", "glutes", "hamstrings"],
  hip_left: ["glutes", "adductors", "abductors", "quads"],
  hip_right: ["glutes", "adductors", "abductors", "quads"],
  knee_left: ["quads", "hamstrings", "calves"],
  knee_right: ["quads", "hamstrings", "calves"],
  ankle_left: ["calves"],
  ankle_right: ["calves"],
};

export function active(restrictions: readonly Restriction[]): readonly Restriction[] {
  return restrictions.filter((r) => r.resolvedOn === null);
}

/** Restrições ativas que envolvem o músculo do exercício (aviso; não bloqueia). */
export function restrictionsFor(
  muscle: string | null,
  restrictions: readonly Restriction[],
): readonly Restriction[] {
  if (muscle === null) {
    return [];
  }
  return active(restrictions).filter(
    (r) => r.bodyRegion !== null && (REGION_MUSCLES[r.bodyRegion] ?? []).includes(muscle),
  );
}
