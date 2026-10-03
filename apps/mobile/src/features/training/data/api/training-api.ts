import {
  addProgramWorkout,
  archiveWorkout,
  createProgram,
  createWorkoutTemplate,
  getActiveProgram,
  getWorkout,
  listWorkoutTemplates,
  saveWorkout,
  schemas,
  updateProgram,
  type ProgramInput as ProgramBody,
  type WorkoutWrite,
} from "@moveup/api-client";
import { z } from "zod";
import { callApi } from "../../../../shared/lib/http";
import type { Program } from "../../domain/program";
import type { TrainingRepository } from "../../domain/ports";
import { newKey, type SetDraft, type Workout, type WorkoutDraft } from "../../domain/workout";

const NoContent = z.undefined();
type WorkoutDto = z.infer<typeof schemas.GetWorkout200Response>;
type ProgramDto = z.infer<typeof schemas.GetProgram200Response>;

const ifMatch = (revision: number) => ({ headers: { "If-Match": `"r${String(revision)}"` } });

/** Sem valor = ausente (a API espera ausência, não null). */
function defined<T extends object>(value: T): T {
  return Object.fromEntries(
    Object.entries(value).filter(([, v]) => v !== null && v !== undefined),
  ) as T;
}

function toWorkout(dto: WorkoutDto): Workout {
  return {
    id: dto.id,
    template: dto.template,
    programId: dto.programId ?? null,
    revision: dto.revision,
    versionId: dto.versionId,
    versionNumber: dto.versionNumber,
    draft: {
      name: dto.name,
      goal: dto.content.goal ?? null,
      estimatedMinutes: dto.content.estimatedMinutes ?? null,
      notes: dto.content.notes ?? null,
      blocks: dto.content.blocks.map((b) => ({
        key: newKey("block"),
        name: b.name ?? null,
        method: b.method as Workout["draft"]["blocks"][number]["method"],
        preset: b.preset === "tabata" ? "tabata" : null,
        rounds: b.rounds ?? null,
        workSeconds: b.workSeconds ?? null,
        restSeconds: b.restSeconds ?? null,
        restBetweenRounds: b.restBetweenRounds ?? null,
        durationSeconds: b.durationSeconds ?? null,
        exercises: b.exercises.map((e) => ({
          key: newKey("exercise"),
          exerciseId: e.exerciseId,
          exerciseName: e.exerciseName ?? "",
          trackingType: e.trackingType ?? "reps_load",
          primaryMuscle: e.primaryMuscle ?? null,
          restSeconds: e.restSeconds ?? null,
          notes: e.notes ?? null,
          sets: e.sets.map((s): SetDraft => ({
            type: s.type ?? "normal",
            repsMin: s.repsMin ?? null,
            repsMax: s.repsMax ?? null,
            loadKg: s.loadKg ?? null,
            durationSeconds: s.durationSeconds ?? null,
            distanceM: s.distanceM ?? null,
            targetRir: s.targetRir ?? null,
            restSeconds: s.restSeconds ?? null,
          })),
        })),
      })),
    },
  };
}

function toBody(draft: WorkoutDraft): WorkoutWrite {
  return {
    name: draft.name.trim(),
    content: defined({
      goal: draft.goal ?? undefined,
      estimatedMinutes: draft.estimatedMinutes ?? undefined,
      notes: draft.notes ?? undefined,
      blocks: draft.blocks.map((b) =>
        defined({
          name: b.name ?? undefined,
          method: b.method,
          preset: b.preset ?? undefined,
          rounds: b.rounds ?? undefined,
          workSeconds: b.workSeconds ?? undefined,
          restSeconds: b.restSeconds ?? undefined,
          restBetweenRounds: b.restBetweenRounds ?? undefined,
          durationSeconds: b.durationSeconds ?? undefined,
          exercises: b.exercises.map((e) =>
            defined({
              exerciseId: e.exerciseId,
              restSeconds: e.restSeconds ?? undefined,
              notes: e.notes ?? undefined,
              sets: e.sets.map((s) =>
                defined({
                  type: s.type,
                  repsMin: s.repsMin ?? undefined,
                  repsMax: s.repsMax ?? undefined,
                  loadKg: s.loadKg ?? undefined,
                  durationSeconds: s.durationSeconds ?? undefined,
                  distanceM: s.distanceM ?? undefined,
                  targetRir: s.targetRir ?? undefined,
                  restSeconds: s.restSeconds ?? undefined,
                }),
              ),
            }),
          ),
        }),
      ),
    }),
  } as WorkoutWrite;
}

function toProgram(dto: ProgramDto): Program {
  return {
    id: dto.id,
    linkId: dto.linkId,
    name: dto.name,
    goal: dto.goal ?? null,
    startsOn: dto.startsOn ?? null,
    endsOn: dto.endsOn ?? null,
    scheduleMode: dto.scheduleMode,
    weeklyTarget: dto.weeklyTarget ?? null,
    revision: dto.revision,
    workouts: dto.workouts.map((w) => ({
      id: w.id,
      name: w.name,
      position: w.position,
      weekdays: w.weekdays,
      estimatedMinutes: w.estimatedMinutes ?? null,
      exercises: w.exercises,
    })),
  };
}

export function createTrainingApiRepository(): TrainingRepository {
  return {
    async listTemplates() {
      const dto = await callApi(
        () => listWorkoutTemplates(),
        schemas.ListWorkoutTemplates200Response,
      );
      return dto.map((t) => ({
        id: t.id,
        name: t.name,
        exercises: t.exercises,
        blocks: t.blocks,
        estimatedMinutes: t.estimatedMinutes ?? null,
        updatedAt: new Date(t.updatedAt),
      }));
    },
    async createTemplate(draft) {
      return toWorkout(
        await callApi(
          () => createWorkoutTemplate(toBody(draft)),
          schemas.CreateWorkoutTemplate201Response,
        ),
      );
    },
    async getWorkout(workoutId) {
      return toWorkout(await callApi(() => getWorkout(workoutId), schemas.GetWorkout200Response));
    },
    async saveWorkout(workoutId, revision, draft) {
      return toWorkout(
        await callApi(
          () => saveWorkout(workoutId, toBody(draft), ifMatch(revision)),
          schemas.SaveWorkout200Response,
        ),
      );
    },
    async archiveWorkout(workoutId, revision) {
      await callApi(() => archiveWorkout(workoutId, ifMatch(revision)), NoContent);
    },
    async activeProgram(linkId) {
      const dto = await callApi(
        () => getActiveProgram(linkId),
        z.union([schemas.GetActiveProgram200Response, z.undefined()]),
      );
      return dto === undefined ? null : toProgram(dto);
    },
    async createProgram(linkId, input) {
      const body = defined({ ...input }) as unknown as ProgramBody;
      return toProgram(
        await callApi(() => createProgram(linkId, body), schemas.CreateProgram201Response),
      );
    },
    async updateProgram(program, input, schedule) {
      const body = {
        ...(defined({ ...input }) as unknown as ProgramBody),
        schedule: schedule.map((s) => ({ workoutId: s.workoutId, weekdays: [...s.weekdays] })),
      };
      return toProgram(
        await callApi(
          () => updateProgram(program.id, body, ifMatch(program.revision)),
          schemas.UpdateProgram200Response,
        ),
      );
    },
    async addProgramWorkout(programId, templateId, name) {
      const body = { templateId, name };
      return toWorkout(
        await callApi(
          () => addProgramWorkout(programId, body),
          schemas.AddProgramWorkout201Response,
        ),
      );
    },
  };
}
