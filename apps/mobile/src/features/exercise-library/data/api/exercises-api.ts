import {
  archiveExercise,
  createExercise,
  schemas,
  searchExercises,
  type NewExercise,
} from "@moveup/api-client";
import { z } from "zod";
import { callApi } from "../../../../shared/lib/http";
import { isMuscleCode, type Exercise } from "../../domain/exercise";
import type { ExercisesRepository } from "../../domain/ports";

const NoContent = z.undefined();
const SEARCH_LIMIT = 60;

type ExerciseDto = z.infer<typeof schemas.SearchExercises200ResponseItem>;

function toExercise(dto: ExerciseDto): Exercise {
  return {
    id: dto.id,
    name: dto.name,
    modality: dto.modality,
    trackingType: dto.trackingType,
    primaryMuscle: isMuscleCode(dto.primaryMuscle) ? dto.primaryMuscle : null,
    // código desconhecido (versão nova da API) é ignorado em vez de quebrar a tela
    secondaryMuscles: dto.secondaryMuscles.filter(isMuscleCode),
    equipment: dto.equipment ?? null,
    unilateral: dto.unilateral,
    instructions: dto.instructions ?? null,
    mediaUrl: dto.mediaUrl ?? null,
    custom: dto.custom,
  };
}

export function createExercisesApiRepository(): ExercisesRepository {
  return {
    async search(query, muscle) {
      const params: { q?: string; muscle?: string; limit: number } = { limit: SEARCH_LIMIT };
      if (query.trim() !== "") {
        params.q = query.trim();
      }
      if (muscle !== null) {
        params.muscle = muscle;
      }
      const dto = await callApi(
        () => searchExercises(params as Parameters<typeof searchExercises>[0]),
        schemas.SearchExercises200Response,
      );
      return dto.map(toExercise);
    },

    async create(input) {
      const body: NewExercise = {
        name: input.name.trim(),
        modality: input.modality,
        trackingType: input.trackingType,
        secondaryMuscles: [...input.secondaryMuscles],
        unilateral: input.unilateral,
      };
      if (input.primaryMuscle !== null) {
        body.primaryMuscle = input.primaryMuscle;
      }
      if (input.equipment !== null && input.equipment.trim() !== "") {
        body.equipment = input.equipment.trim();
      }
      if (input.instructions !== null && input.instructions.trim() !== "") {
        body.instructions = input.instructions.trim();
      }
      if (input.mediaUrl !== null && input.mediaUrl.trim() !== "") {
        body.mediaUrl = input.mediaUrl.trim();
      }
      return toExercise(
        await callApi(() => createExercise(body), schemas.CreateExercise201Response),
      );
    },

    async archive(exerciseId) {
      await callApi(() => archiveExercise(exerciseId), NoContent);
    },
  };
}
