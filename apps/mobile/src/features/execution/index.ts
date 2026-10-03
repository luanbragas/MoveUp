// API pública da feature execution: o treino registrado no aparelho e o envio.
export type { SessionStore, SessionSyncApi } from "./domain/ports";
export type { PlannedInput, Session } from "./domain/session";
export { toSyncPayload } from "./domain/session";
export { useActiveSession, usePushSessions, useSession } from "./hooks/use-sessions";
export { useStartSession } from "./hooks/use-start";
export { AutoPush } from "./ui/AutoPush";
export type { ExerciseInfoLite } from "./ui/ExecutionScreen";
export { HistoryScreen, SessionDetailScreen } from "./ui/HistoryScreen";
export { SessionScreen } from "./ui/SessionScreen";
export { strings as executionStrings } from "./ui/strings";
