import { getSyncChanges, schemas } from "@moveup/api-client";
import type { z } from "zod";
import { callApi } from "../../../../shared/lib/http";
import type { PlannedChanges } from "../../domain/planned";
import type { SyncApi } from "../../domain/ports";

type ChangesDto = z.infer<typeof schemas.GetSyncChanges200Response>;

export function toChanges(dto: ChangesDto): PlannedChanges {
  return {
    cursor: dto.cursor,
    programs: dto.programs.map((p) => ({
      deleted: p.deleted,
      program: {
        id: p.id,
        linkId: p.linkId,
        name: p.name,
        goal: p.goal ?? null,
        startsOn: p.startsOn ?? null,
        endsOn: p.endsOn ?? null,
        scheduleMode: p.scheduleMode,
        weeklyTarget: p.weeklyTarget ?? null,
      },
      workouts: p.workouts.map((w) => ({
        id: w.id,
        programId: p.id,
        name: w.name,
        position: w.position,
        weekdays: w.weekdays,
        versionId: w.versionId,
        versionNumber: w.versionNumber,
        goal: w.content.goal ?? null,
        estimatedMinutes: w.content.estimatedMinutes ?? null,
        notes: w.content.notes ?? null,
        blocks: w.content.blocks.map((b) => ({
          name: b.name ?? null,
          method: b.method,
          preset: b.preset ?? null,
          rounds: b.rounds ?? null,
          workSeconds: b.workSeconds ?? null,
          restSeconds: b.restSeconds ?? null,
          restBetweenRounds: b.restBetweenRounds ?? null,
          durationSeconds: b.durationSeconds ?? null,
          exercises: b.exercises.map((e) => ({
            exerciseId: e.exerciseId,
            restSeconds: e.restSeconds ?? null,
            notes: e.notes ?? null,
            sets: e.sets.map((s) => ({
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
      })),
    })),
    exercises: dto.exercises.map((e) => ({
      id: e.id,
      name: e.name,
      trackingType: e.trackingType,
      primaryMuscle: e.primaryMuscle ?? null,
      secondaryMuscles: e.secondaryMuscles,
      equipment: e.equipment ?? null,
      instructions: e.instructions ?? null,
      mediaUrl: e.mediaUrl ?? null,
    })),
  };
}

export function createSyncApi(): SyncApi {
  return {
    async changesSince(cursor) {
      const dto = await callApi(
        () => getSyncChanges(cursor === null ? undefined : { since: cursor }),
        schemas.GetSyncChanges200Response,
      );
      return toChanges(dto);
    },
  };
}
