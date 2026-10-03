// API pública da feature exercise-library: o que outras features e as rotas podem importar.
export type { ExercisesRepository } from "./domain/ports";
export type { Exercise, MuscleCode } from "./domain/exercise";
export { isMuscleCode, muscleLevels } from "./domain/exercise";
export { useExercisesByMuscle } from "./hooks/use-exercises";
export { ExerciseLibraryScreen } from "./ui/ExerciseLibraryScreen";
export { NewExerciseScreen } from "./ui/NewExerciseScreen";
