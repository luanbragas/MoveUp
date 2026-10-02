import { z } from "zod";
import { ApiFailure, toAppError } from "./app-error";
import { callApi } from "./call-api";

const Schema = z.object({ name: z.string() });

const problem = {
  type: "https://api.moveup.com.br/problems/plan-limit-reached",
  title: "Limite de alunos do plano atingido",
  status: 409,
  detail: "Limite de alunos do plano atingido.",
  code: "plan-limit-reached",
  traceId: "4bf92f3577b34da6a3ce929d0e0e4736",
};

async function errorOf(promise: Promise<unknown>) {
  try {
    await promise;
  } catch (error) {
    return toAppError(error);
  }
  throw new Error("era esperado um erro");
}

describe("callApi", () => {
  it("valida e devolve a resposta 2xx", async () => {
    const result = await callApi(
      () => Promise.resolve({ status: 200, data: { name: "Ana" } }),
      Schema,
    );

    expect(result).toEqual({ name: "Ana" });
  });

  it("resposta fora do contrato vira unexpected", async () => {
    const error = await errorOf(
      callApi(() => Promise.resolve({ status: 200, data: { nome: "Ana" } }), Schema),
    );

    expect(error).toEqual({ kind: "unexpected" });
  });

  it("ProblemDetail vira problem com code, status e traceId", async () => {
    const error = await errorOf(
      callApi(() => Promise.resolve({ status: 409, data: problem }), Schema),
    );

    expect(error).toEqual({
      kind: "problem",
      code: "plan-limit-reached",
      status: 409,
      traceId: problem.traceId,
    });
  });

  it("erro sem ProblemDetail vira unexpected", async () => {
    const error = await errorOf(
      callApi(() => Promise.resolve({ status: 502, data: "<html>Bad Gateway</html>" }), Schema),
    );

    expect(error).toEqual({ kind: "unexpected" });
  });

  it("falha de rede vira network", async () => {
    const error = await errorOf(
      callApi(() => Promise.reject(new TypeError("Network request failed")), Schema),
    );

    expect(error).toEqual({ kind: "network" });
  });

  it("erro de programação não é mascarado como rede", async () => {
    await expect(
      callApi(() => Promise.reject(new Error("api-client não configurado")), Schema),
    ).rejects.not.toBeInstanceOf(ApiFailure);
  });
});
