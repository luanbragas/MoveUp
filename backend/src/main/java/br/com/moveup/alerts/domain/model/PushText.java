package br.com.moveup.alerts.domain.model;

/**
 * Texto do push: sempre neutro. Aparece na tela bloqueada e passa pelos servidores da Apple e do
 * Google, então nunca leva nome de aluno, tipo de alerta de saúde ou número (CLAUDE.md, regra 7).
 */
public final class PushText {

  public static final String TITLE = "MoveUp";
  public static final String BODY = "Um aluno precisa da sua atenção.";

  private PushText() {}
}
