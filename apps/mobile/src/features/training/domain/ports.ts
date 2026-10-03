import type { Program, ProgramInput, ScheduleMode, TemplateSummary } from "./program";
import type { Workout, WorkoutDraft } from "./workout";

/** Treinos, modelos e programas (backend: módulo training). */
export interface TrainingRepository {
  listTemplates(): Promise<readonly TemplateSummary[]>;
  createTemplate(draft: WorkoutDraft): Promise<Workout>;
  getWorkout(workoutId: string): Promise<Workout>;
  /** If-Match com a revisão; 412 version-mismatch se outro aparelho salvou antes. */
  saveWorkout(workoutId: string, revision: number, draft: WorkoutDraft): Promise<Workout>;
  archiveWorkout(workoutId: string, revision: number): Promise<void>;

  /** null = aluno sem programa ativo. */
  activeProgram(linkId: string): Promise<Program | null>;
  createProgram(linkId: string, input: ProgramInput): Promise<Program>;
  updateProgram(
    program: Program,
    input: ProgramInput,
    schedule: readonly { workoutId: string; weekdays: readonly number[] }[],
  ): Promise<Program>;
  addProgramWorkout(
    programId: string,
    templateId: string | null,
    name: string | null,
  ): Promise<Workout>;
}

/** Rascunho do editor guardado no celular: sobrevive à queda da internet e ao app fechado. */
export interface StoredDraft {
  /** Revisão do treino quando o rascunho começou (outra = alguém salvou depois). */
  readonly baseRevision: number;
  readonly draft: WorkoutDraft;
  readonly savedAt: string;
}

export interface WorkoutDraftStore {
  get(workoutId: string): Promise<StoredDraft | null>;
  save(workoutId: string, stored: StoredDraft): Promise<void>;
  remove(workoutId: string): Promise<void>;
  /** Sair da conta. */
  clear(): Promise<void>;
}

export type { ScheduleMode };
