package br.com.moveup.anamnesis.domain.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Respostas já validadas pelo modelo. Guardadas cifradas; objetivo, nível, dias e minutos também
 * vão abertos (colunas de ação, sem dado clínico).
 */
public record Answers(Map<String, Object> values, List<String> parqCodes) {

  public Answers {
    values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    parqCodes = List.copyOf(parqCodes);
  }

  /** Qualquer "sim" no PAR-Q: liberação médica recomendada. */
  public boolean parqPositive() {
    return parqCodes.stream().anyMatch(code -> Boolean.TRUE.equals(values.get(code)));
  }

  public String text(String code) {
    return values.get(code) instanceof String s ? s : null;
  }

  public Integer integer(String code) {
    return values.get(code) instanceof Integer i ? i : null;
  }
}
