// API pública da feature anamnesis: anamnese do aluno, revisão do personal e restrições.
export type { AnamnesisDraftStore, AnamnesisRepository } from "./domain/ports";
export { AnamnesisBanner } from "./ui/AnamnesisBanner";
export { ClientAnamnesisScreen } from "./ui/ClientAnamnesisScreen";
export { RestrictionBanner, RestrictionWarning } from "./ui/RestrictionBanner";
export { ReviewAnamnesisScreen } from "./ui/ReviewAnamnesisScreen";
