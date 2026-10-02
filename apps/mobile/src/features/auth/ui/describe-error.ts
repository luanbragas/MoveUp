import { toAppError } from "../../../shared/lib/http";
import { errorMessage } from "../../../shared/ui/error-messages";
import { AuthFailure } from "../domain/session";
import { strings } from "./strings";

/** Mensagem para o usuário a partir de qualquer erro desta feature (nunca o detalhe técnico). */
export function describeError(error: unknown): string {
  if (error instanceof AuthFailure) {
    return strings.authErrors[error.code];
  }
  return errorMessage(toAppError(error));
}
