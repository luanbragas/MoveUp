import type { Me } from "./me";

/** Porta da conta do usuário: o hook não sabe se vem da API ou de um fake. */
export interface MeRepository {
  getMe(): Promise<Me>;
}
