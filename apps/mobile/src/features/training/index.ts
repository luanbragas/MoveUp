// API pública da feature training: o que outras features e as rotas podem importar.
export type { TrainingRepository } from "./domain/ports";
export { TemplatesScreen } from "./ui/TemplatesScreen";
export { WorkoutEditorRoute } from "./ui/WorkoutEditorScreen";
export { ClientProgramScreen } from "./ui/ClientProgramScreen";
export { PresencialSessionScreen } from "./ui/PresencialSessionScreen";
export { summarize, blockTiming } from "./domain/workout";
export type { ExerciseDraft, BlockDraft, BlockMethod, TrackingType } from "./domain/workout";
export { strings as trainingStrings } from "./ui/strings";
