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

export type { ScheduleMode };
