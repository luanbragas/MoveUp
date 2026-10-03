// API pública da feature exercise-library: o que outras features e as rotas podem importar.
export type { ExercisesRepository } from "./domain/ports";
export type { Exercise, MuscleCode } from "./domain/exercise";
export { muscleLevels } from "./domain/exercise";
export { ExerciseLibraryScreen } from "./ui/ExerciseLibraryScreen";
export { NewExerciseScreen } from "./ui/NewExerciseScreen";
