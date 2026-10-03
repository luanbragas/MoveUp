package br.com.moveup.anamnesis.domain.model;

import br.com.moveup.anamnesis.domain.exception.InvalidAnamnesisData;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Modelo de perguntas (v1: o do sistema, com PAR-Q). */
public record AnamnesisTemplate(UUID id, int version, List<Question> questions) {

  public AnamnesisTemplate {
    questions = List.copyOf(questions);
  }

  /**
   * Respostas conferidas contra o modelo: código desconhecido é recusado, obrigatória precisa estar
   * respondida e cada valor no formato da pergunta.
   */
  public Answers validate(Map<String, Object> raw) {
    var known = questions.stream().map(Question::code).toList();
    for (var code : raw.keySet()) {
      if (!known.contains(code)) {
        throw new InvalidAnamnesisData("answer-unknown", "Pergunta desconhecida: " + code + ".");
      }
    }
    var values = new LinkedHashMap<String, Object>();
    for (var q : questions) {
      var value = q.normalize(raw.get(q.code()));
      if (value == null) {
        if (q.required()) {
          throw new InvalidAnamnesisData(
              "answer-required", "Responda a pergunta " + q.code() + ".");
        }
        continue;
      }
      values.put(q.code(), value);
    }
    return new Answers(values, parqCodes());
  }

  private List<String> parqCodes() {
    return questions.stream().filter(Question::isParq).map(Question::code).toList();
  }
}
